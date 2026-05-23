package br.com.fiap.challenge.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClinicalRecordRequest(
        @NotNull @PastOrPresent LocalDate date,
        @NotBlank @Size(max = 200) String description,
        @Size(max = 300) String diagnosis,
        @Size(max = 500) String treatment,
        @DecimalMin("0.1") @DecimalMax("300.0") BigDecimal weight,
        @Size(max = 1000) String observations,
        @NotNull Long petId,
        Long veterinarianId
) {}
