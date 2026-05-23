package br.com.fiap.challenge.service;

import br.com.fiap.challenge.dto.response.PetHealthSummaryResponse;
import br.com.fiap.challenge.entity.Pet;
import br.com.fiap.challenge.entity.PetStreak;
import br.com.fiap.challenge.enums.PetLevel;
import br.com.fiap.challenge.repository.ClinicalRecordRepository;
import br.com.fiap.challenge.repository.ReminderRepository;
import br.com.fiap.challenge.repository.VaccineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Serviço responsável pelos cálculos de saúde e progresso do pet.
 *
 * É o principal diferencial da plataforma além do CRUD:
 * agrega dados de múltiplas fontes (vacinas, prontuários, streak, lembretes)
 * e produz indicadores acionáveis para o tutor.
 *
 * O resultado é cacheado por pet para evitar recalcular a cada requisição.
 * O cache é invalidado sempre que dados relevantes do pet são alterados
 * (chamada explícita a evictCache nos services de vacina, prontuário e check-in).
 */
@Service
@RequiredArgsConstructor
public class PetHealthService {

    private final PetService petService;
    private final VaccineRepository vaccineRepository;
    private final ClinicalRecordRepository clinicalRecordRepository;
    private final ReminderRepository reminderRepository;

    /**
     * Retorna o resumo de saúde do pet, cacheado por petId.
     *
     * @Cacheable armazena o resultado na primeira chamada e retorna diretamente
     * nas chamadas seguintes, sem executar o método novamente — ideal para
     * cálculos agregados que envolvem múltiplas queries ao banco.
     */
    @Cacheable(value = "petHealthSummary", key = "#petId")
    @Transactional(readOnly = true)
    public PetHealthSummaryResponse getHealthSummary(Long petId) {
        Pet pet = petService.getPetOrThrow(petId);
        LocalDate today = LocalDate.now();

        long overdueVaccines = vaccineRepository.countOverdueByPetId(petId, today);
        long totalVaccines = vaccineRepository.findByPetId(petId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        boolean hasRecentCheckup = clinicalRecordRepository.hasRecentRecord(petId, today.minusMonths(12));
        long totalRecords = clinicalRecordRepository.countByPetId(petId);
        long pendingReminders = reminderRepository.countPendingByPetId(petId);

        PetStreak streak = pet.getStreak();
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int longestStreak = streak != null ? streak.getLongestStreak() : 0;
        PetLevel level = streak != null ? streak.getLevel() : PetLevel.BEGINNER;

        int healthScore = calculateHealthScore(pet, overdueVaccines, hasRecentCheckup, currentStreak, totalVaccines);
        int profileCompletion = calculateProfileCompletion(pet);

        return new PetHealthSummaryResponse(
                pet.getId(), pet.getName(), pet.getSpecies(),
                healthScore, buildHealthMessage(healthScore),
                profileCompletion, buildProfileMessage(profileCompletion),
                currentStreak, longestStreak, level, level.getLabel(),
                (int) totalVaccines, (int) overdueVaccines,
                (int) totalRecords, hasRecentCheckup, (int) pendingReminders
        );
    }

    /**
     * Invalida o cache de saúde do pet.
     * Chamado pelos outros services sempre que vacinas, prontuários ou check-ins
     * são alterados, garantindo que a próxima consulta reflita os dados atuais.
     */
    @CacheEvict(value = "petHealthSummary", key = "#petId")
    public void evictCache(Long petId) {}

    /**
     * Calcula o score de saúde do pet em uma escala de 0 a 100, com base em 4 critérios:
     *
     * 1. Vínculo com veterinário (25 pts) — pet acompanhado por profissional
     * 2. Vacinas em dia (25 pts) — nenhuma dose atrasada; penaliza -5 por dose vencida
     * 3. Consulta recente nos últimos 12 meses (25 pts) — acompanhamento preventivo
     * 4. Streak de atividade >= 7 dias (25 pts) — hábito de cuidado diário
     */
    private int calculateHealthScore(Pet pet, long overdueVaccines, boolean hasRecentCheckup,
                                     int streak, long totalVaccines) {
        int score = 0;

        if (pet.getVeterinarian() != null) score += 25;

        if (totalVaccines > 0 && overdueVaccines == 0) {
            score += 25; // todas as vacinas em dia
        } else if (totalVaccines > 0) {
            score += Math.max(0, 25 - (int)(overdueVaccines * 5)); // desconta 5 pts por dose atrasada
        }

        if (hasRecentCheckup) score += 25;

        if (streak >= 7) score += 25; // ao menos 1 semana de consistência

        return Math.min(100, score); // garante que nunca ultrapasse 100
    }

    /**
     * Calcula o percentual de preenchimento do perfil do pet (0-100%).
     * Avalia 9 campos/dados relevantes — quanto mais completo, melhor o acompanhamento.
     */
    private int calculateProfileCompletion(Pet pet) {
        int total = 9;
        int filled = 0;

        if (pet.getName() != null && !pet.getName().isBlank()) filled++;
        if (pet.getSpecies() != null) filled++;
        if (pet.getBreed() != null && !pet.getBreed().isBlank()) filled++;
        if (pet.getBirthDate() != null) filled++;
        if (pet.getWeight() != null) filled++;
        if (pet.getGender() != null) filled++;
        if (pet.getVeterinarian() != null) filled++;
        if (!pet.getVaccines().isEmpty()) filled++;
        if (!pet.getClinicalRecords().isEmpty()) filled++;

        return (int) Math.round((double) filled / total * 100);
    }

    private String buildHealthMessage(int score) {
        if (score >= 90) return "Excelente! Seu pet está muito saudável.";
        if (score >= 75) return "Ótimo! Seu pet está bem cuidado.";
        if (score >= 50) return "Bom, mas há espaço para melhorar os cuidados.";
        if (score >= 25) return "Atenção: seu pet precisa de mais cuidados preventivos.";
        return "Urgente: agende uma consulta veterinária.";
    }

    private String buildProfileMessage(int completion) {
        if (completion == 100) return "Perfil completo!";
        return completion + "% completo — adicione mais informações para um acompanhamento melhor.";
    }
}
