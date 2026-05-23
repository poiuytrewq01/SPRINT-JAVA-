package br.com.fiap.challenge.dto.response;

import br.com.fiap.challenge.entity.ClinicalRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClinicalRecordResponse(
        Long id,
        LocalDate date,
        String description,
        String diagnosis,
        String treatment,
        BigDecimal weight,
        String observations,
        Long petId,
        String petName,
        Long veterinarianId,
        String veterinarianName,
        LocalDateTime createdAt
) {
    public static ClinicalRecordResponse from(ClinicalRecord r) {
        return new ClinicalRecordResponse(
                r.getId(), r.getDate(), r.getDescription(), r.getDiagnosis(), r.getTreatment(),
                r.getWeight(), r.getObservations(),
                r.getPet() != null ? r.getPet().getId() : null,
                r.getPet() != null ? r.getPet().getName() : null,
                r.getVeterinarian() != null ? r.getVeterinarian().getId() : null,
                r.getVeterinarian() != null ? r.getVeterinarian().getName() : null,
                r.getCreatedAt()
        );
    }
}
