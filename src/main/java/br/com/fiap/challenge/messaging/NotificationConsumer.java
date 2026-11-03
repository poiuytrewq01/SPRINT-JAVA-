package br.com.fiap.challenge.messaging;

import br.com.fiap.challenge.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor da fila de notificacoes.
 *
 * O RabbitMQ empurra a mensagem (modelo push) e o Spring a converte de JSON
 * para NotificationEvent pelo tipo do parametro.
 *
 * Confirmacao (ack) automatica: se o metodo termina sem excecao, o Spring
 * confirma e a mensagem sai da fila. Se lanca excecao, o retry configurado no
 * application.yaml tenta de novo (3 tentativas, com espera crescente). Na
 * ultima falha a mensagem e rejeitada sem reenfileirar e o RabbitMQ a move
 * para a DLQ, onde pode ser inspecionada sem bloquear as demais.
 *
 * A regra de "o que fazer com a mensagem" esta no NotificationService. Este
 * componente so faz a ponte entre o broker e o dominio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void consume(NotificationEvent event) {
        log.info("Notificacao {} ({}) recebida da fila", event.eventId(), event.type());
        notificationService.store(event);
    }
}
