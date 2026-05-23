package br.com.fiap.challenge.entity;

import br.com.fiap.challenge.enums.PetLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Armazena o progresso de gamificação do pet.
 * O streak representa dias consecutivos de check-in de atividade.
 *
 * A lógica de nível (PetLevel) é derivada do currentStreak:
 * 0-6 = Iniciante, 7-13 = Bronze, 14-29 = Prata, ..., 365+ = Lendário.
 *
 * O streak é zerado caso o tutor não realize check-in no dia seguinte ao último registro.
 */
@Entity
@Table(name = "pet_streaks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true garante que exista no máximo 1 streak por pet
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", unique = true, nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Pet pet;

    @Builder.Default
    private int currentStreak = 0;  // dias consecutivos atuais

    @Builder.Default
    private int longestStreak = 0;  // recorde histórico do pet

    private LocalDate lastCheckIn; // usado para calcular se o streak continua ou foi quebrado

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PetLevel level = PetLevel.BEGINNER;

    @Builder.Default
    private int totalCheckIns = 0; // contador cumulativo de todos os check-ins do pet

    // Atualizado automaticamente pelo Hibernate para rastrear a evolução do streak
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
