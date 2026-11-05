package br.com.fiap.challenge.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Topologia do RabbitMQ: exchanges, filas e bindings.
 *
 *   NotificationRelay ──► [pethealth.notifications]  (direct exchange)
 *                                │ routing key "notification.created"
 *                                ▼
 *                       (pethealth.notifications.queue) ──► NotificationConsumer
 *                                │ falhou 3 vezes
 *                                ▼
 *                         [pethealth.notifications.dlx] (direct exchange)
 *                                │ routing key "notification.dead"
 *                                ▼
 *                       (pethealth.notifications.dlq)   -- fica parada para analise
 *
 * Os beans abaixo sao declarados pelo RabbitAdmin (auto-configurado pelo
 * Spring Boot) na primeira conexao com o broker. Nao e preciso criar nada
 * manualmente no painel do RabbitMQ: a topologia e codigo, versionada no Git,
 * da mesma forma que o schema do banco e versionado pelo Flyway.
 *
 * @EnableAsync habilita o @Async usado no NotificationRelay.
 */
@Configuration
@EnableAsync
public class RabbitMQConfig {

    public static final String EXCHANGE = "pethealth.notifications";
    public static final String QUEUE = "pethealth.notifications.queue";
    public static final String ROUTING_KEY = "notification.created";

    public static final String DEAD_LETTER_EXCHANGE = "pethealth.notifications.dlx";
    public static final String DEAD_LETTER_QUEUE = "pethealth.notifications.dlq";
    public static final String DEAD_LETTER_ROUTING_KEY = "notification.dead";

    /**
     * Direct exchange: entrega a mensagem as filas cujo binding tem exatamente
     * a mesma routing key. E o tipo mais simples e basta aqui, porque existe um
     * unico tipo de consumidor. Um fanout ou topic so faria sentido com varios
     * consumidores interessados em subconjuntos diferentes das mensagens.
     */
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    /**
     * Fila principal, duravel (sobrevive a reinicio do broker).
     *
     * Os argumentos x-dead-letter-* dizem ao RabbitMQ para onde mandar uma
     * mensagem rejeitada sem reenfileiramento. Sem isso, uma mensagem que
     * sempre falha (poison message) seria reentregue indefinidamente e
     * travaria o consumo da fila.
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(notificationExchange()).with(ROUTING_KEY);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DEAD_LETTER_ROUTING_KEY);
    }

    /**
     * Serializa as mensagens em JSON em vez da serializacao Java padrao.
     *
     * JSON e legivel no painel do RabbitMQ e nao amarra o formato a classes
     * Java -- outro servico, em qualquer linguagem, conseguiria consumir a fila.
     * O ObjectMapper injetado e o do Spring Boot, que ja sabe lidar com
     * LocalDateTime.
     *
     * Os pacotes confiaveis limitam quais classes podem ser criadas a partir
     * do cabecalho de tipo da mensagem: sem essa restricao, uma mensagem
     * forjada poderia pedir a desserializacao de uma classe arbitraria.
     *
     * O Spring Boot aplica este bean automaticamente ao RabbitTemplate e aos
     * containers dos @RabbitListener.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages(NotificationEvent.class.getPackageName());

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
