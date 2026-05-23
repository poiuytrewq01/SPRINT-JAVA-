package br.com.fiap.challenge.dto.request;

import br.com.fiap.challenge.enums.Gender;
import br.com.fiap.challenge.enums.Species;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PetRequest(
        @NotBlank(message = "Nome é obrigatório") @Size(min = 1, max = 50) String name,
        @NotNull(message = "Espécie é obrigatória") Species species,
        @Size(max = 80) String breed,
        @NotNull(message = "Data de nascimento é obrigatória") @Past(message = "Data de nascimento deve ser no passado") LocalDate birthDate,
        @DecimalMin(value = "0.1", message = "Peso mínimo 0.1 kg") @DecimalMax(value = "300.0", message = "Peso máximo 300 kg") BigDecimal weight,
        Gender gender,
        Boolean profilePublic,
        @NotNull(message = "Tutor é obrigatório") Long tutorId,
        Long veterinarianId
) {}
