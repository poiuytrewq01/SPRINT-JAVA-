package br.com.fiap.challenge.service;

import br.com.fiap.challenge.entity.Notification;
import br.com.fiap.challenge.entity.User;
import br.com.fiap.challenge.exception.ResourceNotFoundException;
import br.com.fiap.challenge.messaging.NotificationEvent;
import br.com.fiap.challenge.repository.NotificationRepository;
import br.com.fiap.challenge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras das notificacoes: gravar o que chega da fila e servir a tela.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Grava a notificacao recebida do RabbitMQ.
     *
     * Idempotente: se o eventId ja existe, a mensagem e uma reentrega e e
     * ignorada. Retornar normalmente faz o Spring confirmar (ack) a mensagem,
     * que sai da fila.
     *
     * Destinatario inexistente nao e uma falha passageira -- tentar de novo
     * daria o mesmo resultado. AmqpRejectAndDontRequeueException deixa
     * explicito que a mensagem nao deve voltar para a fila: ela e rejeitada e
     * o RabbitMQ a move para a DLQ.
     *
     * @return true se gravou, false se era duplicada
     */
    @Transactional
    public boolean store(NotificationEvent event) {
        if (notificationRepository.existsByEventId(event.eventId())) {
            log.info("Notificacao {} ja processada; reentrega ignorada", event.eventId());
            return false;
        }

        User recipient = userRepository.findById(event.recipientUserId())
                .orElseThrow(() -> new AmqpRejectAndDontRequeueException(
                        "Destinatario " + event.recipientUserId() + " nao existe"));

        notificationRepository.save(Notification.builder()
                .eventId(event.eventId())
                .user(recipient)
                .type(event.type())
                .message(event.message())
                .link(event.link())
                .occurredAt(event.occurredAt())
                .build());
        return true;
    }

    @Transactional(readOnly = true)
    public List<Notification> findRecent(Long userId) {
        return notificationRepository.findTop30ByUserIdOrderByOccurredAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    /**
     * Marca a notificacao como lida e devolve o link de destino.
     *
     * O dono e conferido antes: o id vem da URL, e sem essa checagem qualquer
     * usuario poderia marcar (e descobrir o link) da notificacao de outro.
     */
    @Transactional
    public String open(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação", notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Notificacao pertence a outro usuario.");
        }

        if (notification.isUnread()) {
            notification.setReadAt(LocalDateTime.now());
        }
        return notification.getLink();
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }
}
