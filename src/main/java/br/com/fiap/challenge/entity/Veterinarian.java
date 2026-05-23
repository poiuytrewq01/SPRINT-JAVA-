package br.com.fiap.challenge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um médico veterinário cadastrado na plataforma.
 * O vínculo entre vet e pet permite rastrear quem realizou cada procedimento,
 * além de construir o histórico clínico de forma auditável.
 */
@Entity
@Table(name = "veterinarians")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Veterinarian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false)
    private String name;

    // CRMV é o registro único do veterinário no Conselho Regional de Medicina Veterinária
    @NotBlank
    @Column(unique = true, nullable = false, length = 20)
    private String crmv;

    @Email
    private String email;

    @Pattern(regexp = "\\d{10,11}", message = "Telefone deve ter 10 ou 11 dígitos")
    private String phone;

    @Size(max = 100)
    private String specialty;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /*
     * Relacionamento inverso (sem cascade): o vet não gerencia os pets,
     * apenas referencia quais pets estão vinculados a ele.
     * Deletar um vet não deve deletar os pets.
     */
    @OneToMany(mappedBy = "veterinarian", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Pet> pets = new ArrayList<>();
}
