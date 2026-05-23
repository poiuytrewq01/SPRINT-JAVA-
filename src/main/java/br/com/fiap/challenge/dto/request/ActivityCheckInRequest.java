package br.com.fiap.challenge.dto.request;

import br.com.fiap.challenge.enums.ActivityType;
import jakarta.validation.constraints.*;

public record ActivityCheckInRequest(
        @NotNull ActivityType activityType,
        @Min(1) @Max(1440) Integer durationMinutes,
        @Size(max = 500) String notes
) {}
