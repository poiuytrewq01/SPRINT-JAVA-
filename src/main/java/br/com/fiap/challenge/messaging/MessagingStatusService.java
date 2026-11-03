package br.com.fiap.challenge.messaging;

import br.com.fiap.challenge.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.stereotype.Service;

/**
 * Situacao da mensageria para o painel do administrador.
 *
 * Mostra, ao vivo, quantas mensagens estao esperando na fila principal e na
 * DLQ e quantos consumidores estao conectados. Durante a apresentacao isso
 * torna visivel uma parte do sistema que normalmente so aparece no log.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingStatusService {

    private final AmqpAdmin amqpAdmin;
    private final NotificationRepository notificationRepository;

    /**
     * @param online         broker respondeu a consulta
     * @param pendentes      mensagens aguardando na fila principal
     * @param consumidores   consumidores conectados a fila principal
     * @param naDlq          mensagens que esgotaram as tentativas
     * @param entregues      notificacoes ja gravadas no banco pelo consumidor
     */
    public record Status(boolean online, long pendentes, long consumidores, long naDlq, long entregues) {}

    public Status current() {
        long entregues = notificationRepository.count();
        try {
            QueueInformation main = amqpAdmin.getQueueInfo(RabbitMQConfig.QUEUE);
            QueueInformation dlq = amqpAdmin.getQueueInfo(RabbitMQConfig.DEAD_LETTER_QUEUE);

            return new Status(true,
                    main != null ? main.getMessageCount() : 0,
                    main != null ? main.getConsumerCount() : 0,
                    dlq != null ? dlq.getMessageCount() : 0,
                    entregues);

        } catch (AmqpException ex) {
            log.warn("RabbitMQ indisponivel para consulta de status: {}", ex.getMessage());
            return new Status(false, 0, 0, 0, entregues);
        }
    }
}
