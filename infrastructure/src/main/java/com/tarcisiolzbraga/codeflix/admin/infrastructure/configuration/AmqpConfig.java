package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.admin.domain.events.DomainEventPublisher;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.EventRouting;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.OutboxDomainEventPublisher;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.RabbitEventSender;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.persistence.OutboxEventRepository;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.ObjectMapper;

// A topologia é declarada aqui, de forma idempotente, e criada na subida da aplicação. Isso é o que
// a mantém funcionando contra um broker vazio, como o de Testcontainers nos testes.
//
// A MESMA topologia está em .rabbitmq/definitions.json, que o broker do docker compose carrega no
// boot. Mudança de exchange, fila, binding ou routing key tem de ser feita nos dois lugares: se
// divergirem, a aplicação falha com PRECONDITION_FAILED ao declarar algo diferente do que existe.
//
// O agendamento existe por causa do relay da tabela de saída.
@Configuration
@EnableScheduling
@EnableConfigurationProperties(AmqpProperties.class)
public class AmqpConfig {

    @Bean
    DirectExchange videoEventsExchange(final AmqpProperties properties) {
        return new DirectExchange(properties.exchange());
    }

    @Bean
    Queue videoCreatedQueue(final AmqpProperties properties) {
        return new Queue(properties.queues().videoCreated().queue());
    }

    @Bean
    Queue videoEncodedQueue(final AmqpProperties properties) {
        return new Queue(properties.queues().videoEncoded().queue());
    }

    @Bean
    Binding videoCreatedBinding(final AmqpProperties properties, final DirectExchange videoEventsExchange) {
        return BindingBuilder.bind(videoCreatedQueue(properties))
                .to(videoEventsExchange)
                .with(properties.queues().videoCreated().routingKey());
    }

    @Bean
    Binding videoEncodedBinding(final AmqpProperties properties, final DirectExchange videoEventsExchange) {
        return BindingBuilder.bind(videoEncodedQueue(properties))
                .to(videoEventsExchange)
                .with(properties.queues().videoEncoded().routingKey());
    }

    @Bean
    EventRouting eventRouting(final AmqpProperties properties) {
        return new EventRouting(properties.queues().videoCreated().routingKey());
    }

    @Bean
    RabbitEventSender rabbitEventSender(final AmqpProperties properties, final RabbitOperations operations) {
        return new RabbitEventSender(properties.exchange(), operations);
    }

    // O agregado avisa a tabela de saída, não o broker: é o que permite gravar e avisar na mesma
    // transação.
    @Bean
    DomainEventPublisher domainEventPublisher(
            final EventRouting eventRouting,
            final OutboxEventRepository outboxRepository,
            final ObjectMapper objectMapper) {
        return new OutboxDomainEventPublisher(eventRouting, outboxRepository, objectMapper);
    }
}
