package br.com.fiap.challenge.dto.response;

import br.com.fiap.challenge.entity.ActivityCheckIn;
import br.com.fiap.challenge.enums.ActivityType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ActivityCheckInResponse(
        Long id,
        LocalDate date,
        ActivityType activityType,
        Integer durationMinutes,
        String notes,
        Long petId,
        String petName,
        LocalDateTime createdAt
) {
    public static ActivityCheckInResponse from(ActivityCheckIn c) {
        return new ActivityCheckInResponse(
                c.getId(), c.getDate(), c.getActivityType(), c.getDurationMinutes(), c.getNotes(),
                c.getPet() != null ? c.getPet().getId() : null,
                c.getPet() != null ? c.getPet().getName() : null,
                c.getCreatedAt()
        );
    }
}
