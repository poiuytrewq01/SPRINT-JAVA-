package br.com.fiap.challenge.entity;

import br.com.fiap.challenge.enums.LinkRequestStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Solicitacao de vinculo entre um pet e um veterinario.
 *
 * Existe para resolver o problema de "vinculo fraco entre clinica e tutor"
 * levantado na proposta: antes, o campo pets.veterinarian_id era escolhido
 * livremente por quem cadastrava o pet, sem que o profissional soubesse.
 * Agora o vinculo so passa a valer depois que o veterinario aceita, e cada
 * decisao fica registrada com data e justificativa.
 */
@Entity
@Table(name = "vet_link_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VetLinkRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veterinarian_id", nullable = false)
    private Veterinarian veterinarian;

    /** Conta que abriu a solicitacao: preserva a autoria mesmo se o pet trocar de tutor. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    @ToString.Include
    private LinkRequestStatus status = LinkRequestStatus.PENDING;

    @Size(max = 300)
    @Column(length = 300)
    private String message;

    /** Justificativa do veterinario ao aprovar ou recusar. */
    @Size(max = 300)
    @Column(name = "response_note", length = 300)
    private String responseNote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public boolean isPending() {
        return status == LinkRequestStatus.PENDING;
    }
}
