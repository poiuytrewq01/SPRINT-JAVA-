package br.com.fiap.challenge.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record VaccineRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @Size(max = 100) String manufacturer,
        @NotNull @PastOrPresent LocalDate applicationDate,
        LocalDate nextDoseDate,
        @Size(max = 50) String certificateNumber,
        @Size(max = 500) String notes,
        @NotNull Long petId,
        Long veterinarianId
) {}
