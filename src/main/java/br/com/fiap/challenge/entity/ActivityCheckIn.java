package br.com.fiap.challenge.entity;

import br.com.fiap.challenge.enums.ActivityType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro de atividade diária do pet — pilar do sistema de gamificação.
 *
 * A constraint única (pet_id + date) garante no banco que um pet
 * só pode ter um check-in por dia, complementando a validação de negócio no service.
 */
@Entity
@Table(name = "activity_check_ins",
       // constraint de unicidade composta: um check-in por pet por dia
       uniqueConstraints = @UniqueConstraint(columnNames = {"pet_id", "date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private LocalDate date;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType activityType;

    @Min(value = 1, message = "Duração mínima 1 minuto")
    @Max(value = 1440, message = "Duração máxima 1440 minutos") // 24h = 1440 min
    private Integer durationMinutes;

    @Size(max = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Pet pet;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
