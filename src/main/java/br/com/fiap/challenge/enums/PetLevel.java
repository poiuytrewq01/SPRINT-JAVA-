package br.com.fiap.challenge.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Níveis de gamificação baseados no streak de check-ins consecutivos do pet.
 * Inspirado no sistema de ligas do Duolingo, incentiva o cuidado diário contínuo.
 *
 * Cada nível tem um mínimo de dias de streak para ser atingido.
 * A progressão é proporcional: os primeiros níveis são rápidos para motivar,
 * os avançados exigem meses de consistência.
 */
@Getter
@RequiredArgsConstructor
public enum PetLevel {
    BEGINNER("Iniciante", 0),    // 0-6 dias
    BRONZE("Bronze", 7),         // 1 semana
    SILVER("Prata", 14),         // 2 semanas
    GOLD("Ouro", 30),            // 1 mês
    PLATINUM("Platina", 90),     // 3 meses
    DIAMOND("Diamante", 180),    // 6 meses
    LEGENDARY("Lendário", 365);  // 1 ano completo

    private final String label;
    private final int minStreak; // streak mínimo para atingir este nível

    /**
     * Determina o nível atual com base no streak.
     * Percorre todos os níveis em ordem crescente e retorna o maior que o pet atingiu.
     */
    public static PetLevel fromStreak(int streak) {
        PetLevel current = BEGINNER;
        for (PetLevel level : values()) {
            if (streak >= level.minStreak) current = level;
        }
        return current;
    }
}
