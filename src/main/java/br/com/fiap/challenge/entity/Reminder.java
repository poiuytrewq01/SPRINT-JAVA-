package br.com.fiap.challenge.entity;

import br.com.fiap.challenge.enums.ReminderStatus;
import br.com.fiap.challenge.enums.ReminderType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lembrete inteligente vinculado a um pet.
 *
 * Lembretes podem ser criados manualmente ou automaticamente:
 * - Ao registrar uma vacina com nextDoseDate → lembrete do tipo VACCINE
 * - Ao registrar um prontuário → lembrete de RETURN para 6 meses depois
 *
 * O ciclo de vida do status segue: PENDING → SENT | DONE | DISMISSED.
 * Lembretes DISMISSED não podem ser reativados (regra de negócio no ReminderService).
 */
@Entity
@Table(name = "reminders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderType type;

    @NotNull
    @Future // a data do lembrete sempre deve ser futura
    @Column(nullable = false)
    private LocalDate dueDate;

    @NotBlank
    @Size(max = 300)
    @Column(nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReminderStatus status = ReminderStatus.PENDING; // todo lembrete nasce como pendente

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Pet pet;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
