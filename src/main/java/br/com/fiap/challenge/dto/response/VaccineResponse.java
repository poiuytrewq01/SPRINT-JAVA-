package br.com.fiap.challenge.dto.response;

import br.com.fiap.challenge.entity.Vaccine;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VaccineResponse(
        Long id,
        String name,
        String manufacturer,
        LocalDate applicationDate,
        LocalDate nextDoseDate,
        boolean overdue,
        String certificateNumber,
        String notes,
        Long petId,
        String petName,
        Long veterinarianId,
        String veterinarianName,
        LocalDateTime createdAt
) {
    public static VaccineResponse from(Vaccine v) {
        boolean overdue = v.getNextDoseDate() != null && v.getNextDoseDate().isBefore(LocalDate.now());
        return new VaccineResponse(
                v.getId(), v.getName(), v.getManufacturer(), v.getApplicationDate(), v.getNextDoseDate(),
                overdue, v.getCertificateNumber(), v.getNotes(),
                v.getPet() != null ? v.getPet().getId() : null,
                v.getPet() != null ? v.getPet().getName() : null,
                v.getVeterinarian() != null ? v.getVeterinarian().getId() : null,
                v.getVeterinarian() != null ? v.getVeterinarian().getName() : null,
                v.getCreatedAt()
        );
    }
}
