package br.com.fiap.challenge.messaging;

import br.com.fiap.challenge.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mensagem que viaja pela fila de notificacoes.
 *
 * E um record porque e um dado imutavel: depois de publicado, ninguem deve
 * alterar o conteudo de um evento. O mesmo tipo serve aos dois lados -- o
 * produtor serializa em JSON e o consumidor desserializa de volta.
 *
 * eventId identifica o evento de forma unica. O RabbitMQ garante entrega
 * "pelo menos uma vez": numa falha de rede entre o processamento e o ack, a
 * mesma mensagem pode chegar de novo. O consumidor usa o eventId para
 * reconhecer a repeticao e nao gravar a notificacao duas vezes (idempotencia).
 *
 * link e sempre um caminho interno da aplicacao (comeca com "/"), para onde o
 * usuario e levado ao abrir a notificacao.
 */
public record NotificationEvent(
        UUID eventId,
        NotificationType type,
        Long recipientUserId,
        String message,
        String link,
        LocalDateTime occurredAt) {

    /** Cria um evento novo, com identificador e horario gerados agora. */
    public static NotificationEvent of(NotificationType type, Long recipientUserId,
                                       String message, String link) {
        return new NotificationEvent(UUID.randomUUID(), type, recipientUserId,
                message, link, LocalDateTime.now());
    }
}
