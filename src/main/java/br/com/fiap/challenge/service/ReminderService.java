package br.com.fiap.challenge.service;

import br.com.fiap.challenge.dto.request.ReminderRequest;
import br.com.fiap.challenge.dto.response.ReminderResponse;
import br.com.fiap.challenge.entity.Pet;
import br.com.fiap.challenge.entity.Reminder;
import br.com.fiap.challenge.enums.ReminderStatus;
import br.com.fiap.challenge.enums.ReminderType;
import br.com.fiap.challenge.exception.BusinessException;
import br.com.fiap.challenge.exception.ResourceNotFoundException;
import br.com.fiap.challenge.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    /**
     * Agenda um lembrete disparado por outra operacao do sistema -- a proxima
     * dose de uma vacina, o retorno apos uma consulta.
     *
     * A data e verificada antes de gravar. Reminder.dueDate e anotado com
     * @Future, entao uma data passada faria o Hibernate lancar
     * ConstraintViolationException e derrubar a transacao inteira: ao
     * registrar hoje uma consulta de dois anos atras, o retorno calculado
     * (consulta + 6 meses) cairia no passado e o cadastro do prontuario
     * falharia por causa do lembrete. Como um lembrete so faz sentido para
     * alertar sobre algo que ainda vai acontecer, a data vencida simplesmente
     * nao gera lembrete.
     *
     * Concentrar a regra aqui evita repeti-la em cada service que agenda
     * lembretes automaticos.
     *
     * @return o lembrete criado, ou vazio quando a data ja passou
     */
    @Transactional
    public Optional<Reminder> scheduleAutomatic(Pet pet, ReminderType type, LocalDate dueDate, String message) {
        if (dueDate == null || !dueDate.isAfter(LocalDate.now())) {
            return Optional.empty();
        }

        return Optional.of(reminderRepository.save(Reminder.builder()
                .type(type)
                .dueDate(dueDate)
                .message(message)
                .pet(pet)
                .build()));
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

    /**
     * Conclui um lembrete garantindo que ele pertence ao pet informado.
     *
     * Sem essa conferencia, o controller so validaria que o pet e do tutor --
     * o id do lembrete continuaria livre na URL, e trocar esse numero
     * permitiria concluir o lembrete do pet de outra pessoa. Verificar apenas
     * um dos dois identificadores nao protege o par.
     */
    @Transactional
    public ReminderResponse markDoneForPet(Long reminderId, Long petId) {
        Reminder reminder = getReminderOrThrow(reminderId);

        if (!reminder.getPet().getId().equals(petId)) {
            throw new AccessDeniedException("Lembrete nao pertence a este pet.");
        }
        return updateStatus(reminderId, ReminderStatus.DONE);
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
