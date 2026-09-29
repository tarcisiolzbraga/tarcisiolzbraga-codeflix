package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.admin.domain.events.DomainEventPublisher;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.RabbitDomainEventPublisher;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

// A topologia é declarada aqui e criada pelo broker na subida, então o docker compose e o container
// de teste sobem vazios e a aplicação monta o que precisa.
@Configuration
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
    DomainEventPublisher domainEventPublisher(
            final AmqpProperties properties, final RabbitOperations operations, final ObjectMapper objectMapper) {
        return new RabbitDomainEventPublisher(
                properties.exchange(), properties.queues().videoCreated().routingKey(), operations, objectMapper);
    }
}
