package br.com.fiap.challenge.dto.response;

import br.com.fiap.challenge.entity.PetStreak;
import br.com.fiap.challenge.enums.PetLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PetStreakResponse(
        Long petId,
        String petName,
        int currentStreak,
        int longestStreak,
        int totalCheckIns,
        PetLevel level,
        String levelLabel,
        LocalDate lastCheckIn,
        boolean checkedInToday,
        LocalDateTime updatedAt
) {
    public static PetStreakResponse from(PetStreak s) {
        boolean today = s.getLastCheckIn() != null && s.getLastCheckIn().equals(LocalDate.now());
        return new PetStreakResponse(
                s.getPet().getId(), s.getPet().getName(),
                s.getCurrentStreak(), s.getLongestStreak(), s.getTotalCheckIns(),
                s.getLevel(), s.getLevel().getLabel(),
                s.getLastCheckIn(), today, s.getUpdatedAt()
        );
    }
}
