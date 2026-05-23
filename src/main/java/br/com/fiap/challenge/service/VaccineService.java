package br.com.fiap.challenge.service;

import br.com.fiap.challenge.dto.request.VaccineRequest;
import br.com.fiap.challenge.dto.response.VaccineResponse;
import br.com.fiap.challenge.entity.Reminder;
import br.com.fiap.challenge.entity.Vaccine;
import br.com.fiap.challenge.enums.ReminderType;
import br.com.fiap.challenge.exception.ResourceNotFoundException;
import br.com.fiap.challenge.repository.ReminderRepository;
import br.com.fiap.challenge.repository.VaccineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Gerencia o registro e controle de vacinas dos pets.
 *
 * Funcionalidade além do CRUD: ao registrar uma vacina com nextDoseDate,
 * um Reminder do tipo VACCINE é criado automaticamente para alertar
 * o tutor na data da próxima dose — eliminando o esquecimento manual.
 */
@Service
@RequiredArgsConstructor
public class VaccineService {

    private final VaccineRepository vaccineRepository;
    private final ReminderRepository reminderRepository;
    private final PetService petService;
    private final VeterinarianService veterinarianService;
    private final PetHealthService petHealthService;

    @Transactional(readOnly = true)
    public Page<VaccineResponse> findByPet(Long petId, Pageable pageable) {
        petService.getPetOrThrow(petId); // valida existência do pet antes de buscar vacinas
        return vaccineRepository.findByPetId(petId, pageable).map(VaccineResponse::from);
    }

    @Transactional(readOnly = true)
    public VaccineResponse findById(Long id) {
        return VaccineResponse.from(getVaccineOrThrow(id));
    }

    /** Retorna vacinas cuja próxima dose já venceu — útil para alertas de atenção. */
    @Transactional(readOnly = true)
    public List<VaccineResponse> findOverdue(Long petId) {
        petService.getPetOrThrow(petId);
        return vaccineRepository.findOverdueByPetId(petId, LocalDate.now())
                .stream().map(VaccineResponse::from).toList();
    }

    /** Retorna vacinas com dose prevista dentro dos próximos N dias. */
    @Transactional(readOnly = true)
    public List<VaccineResponse> findUpcoming(Long petId, int daysAhead) {
        petService.getPetOrThrow(petId);
        return vaccineRepository.findUpcomingByPetId(petId, LocalDate.now(), LocalDate.now().plusDays(daysAhead))
                .stream().map(VaccineResponse::from).toList();
    }

    /**
     * Registra uma vacina e, se houver data de próxima dose,
     * cria um lembrete automático para o pet — tudo na mesma transação.
     * Se qualquer parte falhar, ambas as operações são revertidas (rollback).
     */
    @Transactional
    public VaccineResponse create(VaccineRequest request) {
        Vaccine vaccine = Vaccine.builder()
                .name(request.name())
                .manufacturer(request.manufacturer())
                .applicationDate(request.applicationDate())
                .nextDoseDate(request.nextDoseDate())
                .certificateNumber(request.certificateNumber())
                .notes(request.notes())
                .pet(petService.getPetOrThrow(request.petId()))
                .veterinarian(request.veterinarianId() != null
                        ? veterinarianService.getVetOrThrow(request.veterinarianId()) : null)
                .build();

        vaccine = vaccineRepository.save(vaccine);

        // Criação automática de lembrete ao informar a data da próxima dose
        if (request.nextDoseDate() != null) {
            Reminder reminder = Reminder.builder()
                    .type(ReminderType.VACCINE)
                    .dueDate(request.nextDoseDate())
                    .message("Próxima dose de " + request.name() + " para " + vaccine.getPet().getName())
                    .pet(vaccine.getPet())
                    .build();
            reminderRepository.save(reminder);
        }

        // Invalida o cache pois o total/status de vacinas impacta o health score
        petHealthService.evictCache(request.petId());
        return VaccineResponse.from(vaccine);
    }

    @Transactional
    public VaccineResponse update(Long id, VaccineRequest request) {
        Vaccine vaccine = getVaccineOrThrow(id);

        vaccine.setName(request.name());
        vaccine.setManufacturer(request.manufacturer());
        vaccine.setApplicationDate(request.applicationDate());
        vaccine.setNextDoseDate(request.nextDoseDate());
        vaccine.setCertificateNumber(request.certificateNumber());
        vaccine.setNotes(request.notes());
        vaccine.setVeterinarian(request.veterinarianId() != null
                ? veterinarianService.getVetOrThrow(request.veterinarianId()) : null);

        petHealthService.evictCache(vaccine.getPet().getId());
        return VaccineResponse.from(vaccineRepository.save(vaccine));
    }

    @Transactional
    public void delete(Long id) {
        Vaccine vaccine = getVaccineOrThrow(id);
        Long petId = vaccine.getPet().getId();
        vaccineRepository.deleteById(id);
        petHealthService.evictCache(petId);
    }

    private Vaccine getVaccineOrThrow(Long id) {
        return vaccineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vacina", id));
    }
}
