package br.com.fiap.challenge.dto.request;

import br.com.fiap.challenge.enums.ReminderType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record ReminderRequest(
        @NotNull ReminderType type,
        @NotNull @Future LocalDate dueDate,
        @NotBlank @Size(max = 300) String message,
        @NotNull Long petId
) {}
