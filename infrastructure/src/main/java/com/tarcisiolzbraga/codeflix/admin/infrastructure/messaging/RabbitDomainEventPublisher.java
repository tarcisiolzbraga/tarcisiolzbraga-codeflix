package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging;

import com.tarcisiolzbraga.codeflix.admin.domain.events.DomainEvent;
import com.tarcisiolzbraga.codeflix.admin.domain.events.DomainEventPublisher;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaCreated;
import java.util.Objects;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import tools.jackson.databind.ObjectMapper;

// Leva o evento de domínio para a fila como JSON. A chave de roteamento vem do tipo do evento, que
// é o que decide para onde ele vai; evento sem destino é erro de programação, não de dados.
public class RabbitDomainEventPublisher implements DomainEventPublisher {

    private static final String UNKNOWN_EVENT_MESSAGE = "no routing key configured for %s";

    private final String exchange;
    private final String videoCreatedRoutingKey;
    private final RabbitOperations operations;
    private final ObjectMapper objectMapper;

    public RabbitDomainEventPublisher(
            final String exchange,
            final String videoCreatedRoutingKey,
            final RabbitOperations operations,
            final ObjectMapper objectMapper) {
        this.exchange = Objects.requireNonNull(exchange, "'exchange' should not be null");
        this.videoCreatedRoutingKey =
                Objects.requireNonNull(videoCreatedRoutingKey, "'videoCreatedRoutingKey' should not be null");
        this.operations = Objects.requireNonNull(operations, "'operations' should not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "'objectMapper' should not be null");
    }

    @Override
    public void publishEvent(final DomainEvent event) {
        this.operations.convertAndSend(
                this.exchange, routingKeyOf(event), this.objectMapper.writeValueAsString(event));
    }

    private String routingKeyOf(final DomainEvent event) {
        if (event instanceof VideoMediaCreated) {
            return this.videoCreatedRoutingKey;
        }
        throw new IllegalArgumentException(UNKNOWN_EVENT_MESSAGE.formatted(event.getClass().getSimpleName()));
    }
}
