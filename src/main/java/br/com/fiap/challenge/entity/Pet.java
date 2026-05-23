package br.com.fiap.challenge.entity;

import br.com.fiap.challenge.enums.Gender;
import br.com.fiap.challenge.enums.Species;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade central do sistema. O Pet é o elo entre o tutor, o veterinário
 * e todo o histórico clínico (vacinas, prontuários, check-ins e lembretes).
 *
 * Healthscore e profileCompletion são calculados dinamicamente pelo
 * PetHealthService para evitar dados desatualizados persistidos.
 */
@Entity
@Table(name = "pets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 1, max = 50)
    @Column(nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING) // STRING mantém legibilidade no banco; ORDINAL quebraria ao reordenar o enum
    @Column(nullable = false)
    private Species species;

    @Size(max = 80)
    private String breed;

    @NotNull
    @Past // data de nascimento deve ser no passado
    private LocalDate birthDate;

    @DecimalMin(value = "0.1", message = "Peso mínimo 0.1 kg")
    @DecimalMax(value = "300.0", message = "Peso máximo 300 kg")
    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Builder.Default
    private boolean profilePublic = false; // perfil privado por padrão (LGPD)

    // Relacionamento N:1 — muitos pets pertencem a um tutor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Tutor tutor;

    // Relacionamento N:1 — veterinário principal do pet (pode ser nulo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinarian_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Veterinarian veterinarian;

    /*
     * Relacionamento 1:1 com PetStreak.
     * mappedBy indica que a chave estrangeira está na tabela pet_streaks,
     * não aqui. Cascade ALL garante que ao deletar o pet, o streak é removido.
     */
    @OneToOne(mappedBy = "pet", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PetStreak streak;

    // Todas as coleções são LAZY para evitar o problema N+1 nas listagens
    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Vaccine> vaccines = new ArrayList<>();

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ClinicalRecord> clinicalRecords = new ArrayList<>();

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ActivityCheckIn> checkIns = new ArrayList<>();

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Reminder> reminders = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Atualizado automaticamente pelo Hibernate sempre que o pet for salvo
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
