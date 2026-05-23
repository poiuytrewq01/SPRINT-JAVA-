package br.com.fiap.challenge.service;

import br.com.fiap.challenge.dto.request.ReminderRequest;
import br.com.fiap.challenge.dto.response.ReminderResponse;
import br.com.fiap.challenge.entity.Reminder;
import br.com.fiap.challenge.enums.ReminderStatus;
import br.com.fiap.challenge.enums.ReminderType;
import br.com.fiap.challenge.exception.BusinessException;
import br.com.fiap.challenge.exception.ResourceNotFoundException;
import br.com.fiap.challenge.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Gerencia os lembretes inteligentes de saúde do pet.
 *
 * Lembretes podem ser criados de duas formas:
 * - Automaticamente: pelo VaccineService (próxima dose) e ClinicalRecordService (retorno semestral)
 * - Manualmente: pelo tutor via API
 *
 * Regra de negócio: lembretes DISMISSED são imutáveis — representam
 * uma decisão consciente do tutor de descartar aquele alerta.
 */
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final PetService petService;

    @Transactional(readOnly = true)
    public Page<ReminderResponse> findByPet(Long petId, ReminderStatus status, ReminderType type, Pageable pageable) {
        petService.getPetOrThrow(petId);
        return reminderRepository.findByPetIdAndFilters(petId, status, type, pageable)
                .map(ReminderResponse::from);
    }

    @Transactional(readOnly = true)
    public ReminderResponse findById(Long id) {
        return ReminderResponse.from(getReminderOrThrow(id));
    }

    /**
     * Retorna lembretes pendentes dentro de N dias — útil para exibir
     * alertas na tela inicial do app, mostrando o que precisa de atenção em breve.
     */
    @Transactional(readOnly = true)
    public List<ReminderResponse> findUpcoming(Long petId, int daysAhead) {
        petService.getPetOrThrow(petId);
        return reminderRepository.findUpcoming(petId, LocalDate.now(), LocalDate.now().plusDays(daysAhead))
                .stream().map(ReminderResponse::from).toList();
    }

    @Transactional
    public ReminderResponse create(ReminderRequest request) {
        Reminder reminder = Reminder.builder()
                .type(request.type())
                .dueDate(request.dueDate())
                .message(request.message())
                .pet(petService.getPetOrThrow(request.petId()))
                .build();

        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    /**
     * Atualiza o status do lembrete respeitando a regra de imutabilidade do DISMISSED.
     * Usar PATCH (não PUT) reflete que apenas um campo está sendo alterado — boa prática REST.
     */
    @Transactional
    public ReminderResponse updateStatus(Long id, ReminderStatus status) {
        Reminder reminder = getReminderOrThrow(id);

        // Lembrete descartado não pode ser reativado — decisão intencional do tutor
        if (reminder.getStatus() == ReminderStatus.DISMISSED)
            throw new BusinessException("Lembrete já foi descartado e não pode ser alterado.");

        reminder.setStatus(status);
        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    @Transactional
    public void delete(Long id) {
        getReminderOrThrow(id);
        reminderRepository.deleteById(id);
    }

    private Reminder getReminderOrThrow(Long id) {
        return reminderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lembrete", id));
    }
}
