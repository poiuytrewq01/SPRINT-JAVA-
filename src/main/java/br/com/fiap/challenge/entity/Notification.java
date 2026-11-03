package br.com.fiap.challenge.entity;

import br.com.fiap.challenge.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notificacao entregue a um usuario, gravada pelo NotificationConsumer.
 *
 * E o ponto final do caminho assincrono: o evento nasce num service, passa
 * pelo RabbitMQ e termina aqui, quando o consumidor o persiste para que a tela
 * de notificacoes possa exibi-lo.
 *
 * event_id tem constraint unica (migration V5). E a garantia, no banco, de que
 * a mesma mensagem entregue duas vezes nao vira duas notificacoes.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    @ToString.Include
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @ToString.Include
    private NotificationType type;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(nullable = false, length = 200)
    private String link;

    /** Momento em que o fato aconteceu, nao o momento em que a fila entregou. */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** Nulo enquanto o usuario nao abriu a notificacao. */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    public boolean isUnread() {
        return readAt == null;
    }
}
