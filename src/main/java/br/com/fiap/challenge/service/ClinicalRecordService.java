package br.com.fiap.challenge.service;

import br.com.fiap.challenge.dto.request.ClinicalRecordRequest;
import br.com.fiap.challenge.dto.response.ClinicalRecordResponse;
import br.com.fiap.challenge.entity.ClinicalRecord;
import br.com.fiap.challenge.enums.ReminderType;
import br.com.fiap.challenge.exception.ResourceNotFoundException;
import br.com.fiap.challenge.repository.ClinicalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gerencia o histórico clínico (prontuários) dos pets.
 *
 * Funcionalidade além do CRUD: ao criar um prontuário, um lembrete de retorno
 * é gerado automaticamente para 6 meses após a consulta. Isso garante
 * acompanhamento preventivo contínuo sem depender da memória do tutor.
 *
 * A busca por keyword permite localizar prontuários por descrição ou diagnóstico,
 * facilitando o acesso ao histórico clínico completo do pet.
 */
@Service
@RequiredArgsConstructor
public class ClinicalRecordService {

    /** Intervalo padrão de retorno recomendado após uma consulta. */
    private static final int FOLLOW_UP_MONTHS = 6;

    private final ClinicalRecordRepository clinicalRecordRepository;
    private final ReminderService reminderService;
    private final PetService petService;
    private final VeterinarianService veterinarianService;
    private final PetHealthService petHealthService;

    /**
     * Lista prontuários com suporte a busca por palavra-chave em descrição e diagnóstico.
     * O parâmetro keyword é opcional — sem ele, retorna todos os prontuários do pet.
     */
    @Transactional(readOnly = true)
    public Page<ClinicalRecordResponse> findByPet(Long petId, String keyword, Pageable pageable) {
        petService.getPetOrThrow(petId);
        return clinicalRecordRepository.findByPetIdAndKeyword(petId, keyword, pageable)
                .map(ClinicalRecordResponse::from);
    }

    @Transactional(readOnly = true)
    public ClinicalRecordResponse findById(Long id) {
        return ClinicalRecordResponse.from(getRecordOrThrow(id));
    }

    /**
     * Cria um prontuário e dispara automaticamente um lembrete de retorno.
     * Ambas as operações ocorrem na mesma transação para garantir consistência.
     *
     * O retorno em 6 meses segue a recomendação padrão de check-up semestral.
     */
    @Transactional
    public ClinicalRecordResponse create(ClinicalRecordRequest request) {
        ClinicalRecord record = ClinicalRecord.builder()
                .date(request.date())
                .description(request.description())
                .diagnosis(request.diagnosis())
                .treatment(request.treatment())
                .weight(request.weight())
                .observations(request.observations())
                .pet(petService.getPetOrThrow(request.petId()))
                .veterinarian(request.veterinarianId() != null
                        ? veterinarianService.getVetOrThrow(request.veterinarianId()) : null)
                .build();

        record = clinicalRecordRepository.save(record);

        // Lembrete automático de retorno: padrão semestral de acompanhamento
        // preventivo. Um prontuário antigo, cadastrado para completar o
        // histórico, produz uma data de retorno já vencida — nesse caso o
        // ReminderService não agenda nada.
        reminderService.scheduleAutomatic(
                record.getPet(),
                ReminderType.RETURN,
                request.date().plusMonths(FOLLOW_UP_MONTHS),
                "Retorno de " + record.getPet().getName() + ": " + request.description());

        // Prontuário recente impacta o health score (critério de checkup nos últimos 12 meses)
        petHealthService.evictCache(request.petId());
        return ClinicalRecordResponse.from(record);
    }

    @Transactional
    public ClinicalRecordResponse update(Long id, ClinicalRecordRequest request) {
        ClinicalRecord record = getRecordOrThrow(id);

        record.setDate(request.date());
        record.setDescription(request.description());
        record.setDiagnosis(request.diagnosis());
        record.setTreatment(request.treatment());
        record.setWeight(request.weight());
        record.setObservations(request.observations());
        record.setVeterinarian(request.veterinarianId() != null
                ? veterinarianService.getVetOrThrow(request.veterinarianId()) : null);

        petHealthService.evictCache(record.getPet().getId());
        return ClinicalRecordResponse.from(clinicalRecordRepository.save(record));
    }

    @Transactional
    public void delete(Long id) {
        ClinicalRecord record = getRecordOrThrow(id);
        Long petId = record.getPet().getId();
        clinicalRecordRepository.deleteById(id);
        petHealthService.evictCache(petId);
    }

    private ClinicalRecord getRecordOrThrow(Long id) {
        return clinicalRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário", id));
    }
}
