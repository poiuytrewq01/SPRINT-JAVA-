package br.com.fiap.challenge.dto.response;

import br.com.fiap.challenge.entity.Reminder;
import br.com.fiap.challenge.enums.ReminderStatus;
import br.com.fiap.challenge.enums.ReminderType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record ReminderResponse(
        Long id,
        ReminderType type,
        LocalDate dueDate,
        long daysUntilDue,
        String message,
        ReminderStatus status,
        Long petId,
        String petName,
        LocalDateTime createdAt
) {
    public static ReminderResponse from(Reminder r) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), r.getDueDate());
        return new ReminderResponse(
                r.getId(), r.getType(), r.getDueDate(), days, r.getMessage(), r.getStatus(),
                r.getPet() != null ? r.getPet().getId() : null,
                r.getPet() != null ? r.getPet().getName() : null,
                r.getCreatedAt()
        );
    }
}
