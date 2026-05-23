package br.com.fiap.challenge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro de vacinação do pet, com suporte a certificado e controle de próxima dose.
 *
 * Quando nextDoseDate é informado, o VaccineService cria automaticamente
 * um Reminder do tipo VACCINE para alertar o tutor na data correta.
 *
 * O campo overdue é calculado no DTO (VaccineResponse) e não armazenado,
 * pois depende da data atual — evita dados stale no banco.
 */
@Entity
@Table(name = "vaccines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vaccine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false)
    private String name;

    @Size(max = 100)
    private String manufacturer;

    @NotNull
    @PastOrPresent // a data de aplicação não pode ser no futuro
    @Column(nullable = false)
    private LocalDate applicationDate;

    private LocalDate nextDoseDate; // opcional; se preenchido, aciona criação de lembrete

    @Size(max = 50)
    private String certificateNumber;

    @Size(max = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Pet pet;

    // Veterinário que aplicou a vacina (opcional — pode ter sido aplicado pelo tutor)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinarian_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Veterinarian veterinarian;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
