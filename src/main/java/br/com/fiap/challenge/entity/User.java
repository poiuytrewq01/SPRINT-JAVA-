package br.com.fiap.challenge.entity;

import br.com.fiap.challenge.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Conta de acesso a plataforma.
 *
 * Autenticacao e dominio sao separados de proposito: esta entidade responde
 * "quem esta acessando e o que pode fazer", enquanto Tutor e Veterinarian
 * continuam respondendo "quem e essa pessoa no negocio".
 *
 * A alternativa -- colocar senha e perfil dentro de Tutor e Veterinarian --
 * duplicaria os campos de credencial em duas tabelas e obrigaria o
 * UserDetailsService a consultar as duas a cada login.
 *
 * O vinculo com o registro de dominio e opcional porque o perfil ADMIN nao
 * corresponde a nenhum tutor ou veterinario. A constraint ck_users_role_link
 * (migration V2) garante no banco que a combinacao perfil/vinculo e coerente.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    @ToString.Include
    private String name;

    @NotBlank
    @Email
    @Column(unique = true, nullable = false, length = 150)
    @ToString.Include
    private String email;

    /** Sempre o hash BCrypt, nunca a senha digitada. Fora do toString por seguranca. */
    @NotBlank
    @Column(nullable = false, length = 100)
    private String password;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ToString.Include
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", unique = true)
    private Tutor tutor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinarian_id", unique = true)
    private Veterinarian veterinarian;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public boolean isTutor() {
        return role == Role.TUTOR;
    }

    public boolean isVeterinarian() {
        return role == Role.VETERINARIAN;
    }
}
