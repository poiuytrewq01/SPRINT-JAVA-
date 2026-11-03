package br.com.fiap.challenge.messaging;

import br.com.fiap.challenge.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationRelayTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationRelay relay;

    private final NotificationEvent event = NotificationEvent.of(
            NotificationType.LINK_REQUESTED, 3L, "Maria pediu vínculo de Mia com você.", "/vet/solicitacoes");

    @Test
    void publicaNaExchangeComARoutingKeyDasNotificacoes() {
        relay.send(event);

        verify(rabbitTemplate).convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
    }

    /** Broker fora do ar nao pode virar erro na tela de quem aprovou o vinculo. */
    @Test
    void brokerIndisponivelNaoPropagaExcecao() {
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), any(Object.class));

        assertThatCode(() -> relay.send(event)).doesNotThrowAnyException();
    }
}
