package br.com.fiap.challenge.service;

import br.com.fiap.challenge.dto.request.ActivityCheckInRequest;
import br.com.fiap.challenge.dto.response.ActivityCheckInResponse;
import br.com.fiap.challenge.dto.response.PetStreakResponse;
import br.com.fiap.challenge.entity.ActivityCheckIn;
import br.com.fiap.challenge.entity.Pet;
import br.com.fiap.challenge.entity.PetStreak;
import br.com.fiap.challenge.enums.PetLevel;
import br.com.fiap.challenge.exception.BusinessException;
import br.com.fiap.challenge.exception.ResourceNotFoundException;
import br.com.fiap.challenge.repository.ActivityCheckInRepository;
import br.com.fiap.challenge.repository.PetStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Gerencia o sistema de gamificação da plataforma.
 *
 * O check-in diário é o coração do engajamento: o tutor registra uma atividade
 * do pet (passeio, banho, alimentação etc.) e recebe feedback visual através
 * do streak e do nível do pet — incentivando o cuidado contínuo.
 *
 * Regra central: apenas um check-in por pet por dia (validado no service e
 * garantido pela constraint unique no banco).
 */
@Service
@RequiredArgsConstructor
public class ActivityCheckInService {

    private final ActivityCheckInRepository checkInRepository;
    private final PetStreakRepository streakRepository;
    private final PetService petService;
    private final PetHealthService petHealthService;

    @Transactional(readOnly = true)
    public Page<ActivityCheckInResponse> findByPet(Long petId, LocalDate start, LocalDate end, Pageable pageable) {
        petService.getPetOrThrow(petId); // garante que o pet existe antes de consultar
        if (start != null && end != null) {
            return checkInRepository.findByPetIdAndDateRange(petId, start, end, pageable)
                    .map(ActivityCheckInResponse::from);
        }
        return checkInRepository.findByPetIdOrderByDateDesc(petId, pageable).map(ActivityCheckInResponse::from);
    }

    /**
     * Realiza o check-in diário do pet e atualiza o streak de gamificação.
     *
     * Fluxo:
     * 1. Valida que o pet não fez check-in hoje (regra de negócio)
     * 2. Persiste o check-in
     * 3. Atualiza o streak e recalcula o nível
     * 4. Invalida o cache de saúde do pet (score pode ter mudado)
     */
    @Transactional
    public ActivityCheckInResponse checkIn(Long petId, ActivityCheckInRequest request) {
        Pet pet = petService.getPetOrThrow(petId);

        // Validação de negócio: limite de 1 check-in por dia por pet
        if (checkInRepository.existsByPetIdAndDate(petId, LocalDate.now()))
            throw new BusinessException("Check-in já realizado hoje para " + pet.getName());

        ActivityCheckIn checkIn = ActivityCheckIn.builder()
                .date(LocalDate.now())
                .activityType(request.activityType())
                .durationMinutes(request.durationMinutes())
                .notes(request.notes())
                .pet(pet)
                .build();

        checkIn = checkInRepository.save(checkIn);
        updateStreak(pet);

        // Invalida cache pois o streak pode ter cruzado o limiar de 7 dias (impacta health score)
        petHealthService.evictCache(petId);

        return ActivityCheckInResponse.from(checkIn);
    }

    @Transactional(readOnly = true)
    public PetStreakResponse getStreak(Long petId) {
        petService.getPetOrThrow(petId);
        PetStreak streak = streakRepository.findByPetId(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Streak do pet não encontrado. Faça um check-in primeiro."));
        return PetStreakResponse.from(streak);
    }

    @Transactional
    public void delete(Long id) {
        ActivityCheckIn checkIn = checkInRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Check-in", id));
        Long petId = checkIn.getPet().getId();
        checkInRepository.deleteById(id);
        petHealthService.evictCache(petId); // recalcula saúde sem este check-in
    }

    /**
     * Atualiza o streak do pet após um check-in bem-sucedido.
     *
     * Lógica do streak:
     * - Se o último check-in foi ontem → incrementa (dias consecutivos)
     * - Se não houve check-in ontem → zera e reinicia em 1
     * - Atualiza o recorde histórico (longestStreak) se superado
     * - Recalcula o nível (PetLevel) com base no streak atual
     */
    private void updateStreak(Pet pet) {
        PetStreak streak = streakRepository.findByPetId(pet.getId()).orElse(null);

        if (streak == null) {
            // Primeiro check-in do pet — cria o streak inicial
            streak = PetStreak.builder()
                    .pet(pet)
                    .currentStreak(1)
                    .longestStreak(1)
                    .lastCheckIn(LocalDate.now())
                    .totalCheckIns(1)
                    .level(PetLevel.BEGINNER)
                    .build();
        } else {
            LocalDate yesterday = LocalDate.now().minusDays(1);

            if (yesterday.equals(streak.getLastCheckIn())) {
                // Check-in consecutivo: mantém o streak crescendo
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            } else {
                // Streak quebrado (pulou pelo menos 1 dia): reinicia do zero
                streak.setCurrentStreak(1);
            }

            // Atualiza o recorde se o streak atual o superou
            if (streak.getCurrentStreak() > streak.getLongestStreak()) {
                streak.setLongestStreak(streak.getCurrentStreak());
            }

            streak.setLastCheckIn(LocalDate.now());
            streak.setTotalCheckIns(streak.getTotalCheckIns() + 1);
            streak.setLevel(PetLevel.fromStreak(streak.getCurrentStreak()));
        }

        streakRepository.save(streak);
    }
}
