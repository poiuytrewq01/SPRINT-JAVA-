package br.com.fiap.challenge.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Produtor do RabbitMQ: leva os eventos do Spring ate o broker.
 *
 * AFTER_COMMIT: o metodo so roda se a transacao que publicou o evento for
 * confirmada. Em caso de rollback, o evento e descartado e nada sai da
 * aplicacao.
 *
 * @Async: o envio acontece em outra thread. O usuario recebe a resposta da
 * tela sem esperar a conversa com o broker -- se o RabbitMQ estiver lento ou
 * fora do ar, quem sente e a notificacao, nao a aprovacao do vinculo.
 *
 * Limite conhecido: se o broker estiver fora do ar no momento do envio, a
 * notificacao e perdida (fica registrada no log). A operacao de negocio ja
 * foi gravada e nao e desfeita por isso. A evolucao natural seria o padrao
 * Transactional Outbox: gravar o evento numa tabela na mesma transacao e
 * publicar a partir dela.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRelay {

    private final RabbitTemplate rabbitTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
            log.info("Notificacao {} ({}) publicada para o usuario {}",
                    event.eventId(), event.type(), event.recipientUserId());

        } catch (AmqpException ex) {
            log.error("Falha ao publicar a notificacao {} ({}): {}",
                    event.eventId(), event.type(), ex.getMessage());
        }
    }
}
