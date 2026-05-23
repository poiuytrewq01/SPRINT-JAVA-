package br.com.fiap.challenge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa o tutor (dono) de um ou mais pets.
 * É a entidade raiz da hierarquia: um tutor cadastra pets, que possuem todo o histórico clínico.
 */
@Entity
@Table(name = "tutors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Email
    // unique = true garante integridade no banco além da validação de negócio no service
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    @Pattern(regexp = "\\d{10,11}", message = "Telefone deve ter 10 ou 11 dígitos")
    private String phone;

    @NotBlank
    @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos")
    @Column(unique = true, nullable = false, length = 11)
    private String cpf;

    // Preenchido automaticamente pelo Hibernate na inserção; nunca atualizado
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /*
     * FetchType.LAZY: evita o carregamento automático de todos os pets ao buscar um tutor.
     * Como a lista de pets só é necessária em operações específicas, o carregamento
     * sob demanda (lazy) previne consultas desnecessárias ao banco.
     *
     * CascadeType.ALL + orphanRemoval: ao deletar um tutor, todos os seus pets
     * (e cascateando, o histórico deles) são removidos automaticamente.
     */
    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude   // evita StackOverflow no Lombok ao imprimir a entidade
    @EqualsAndHashCode.Exclude
    private List<Pet> pets = new ArrayList<>();
}
