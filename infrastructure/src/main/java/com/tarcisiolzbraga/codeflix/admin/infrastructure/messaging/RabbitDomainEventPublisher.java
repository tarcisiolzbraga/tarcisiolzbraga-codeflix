package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging;

import com.tarcisiolzbraga.codeflix.admin.domain.events.DomainEvent;
import com.tarcisiolzbraga.codeflix.admin.domain.events.DomainEventPublisher;
import java.util.Objects;
import tools.jackson.databind.ObjectMapper;

// Junta as duas metades: descobre o destino e entrega na hora. É o caminho direto, sem fila de
// saída, que continua valendo enquanto o relay não existir.
public class RabbitDomainEventPublisher implements DomainEventPublisher {

    private final EventRouting routing;
    private final RabbitEventSender sender;
    private final ObjectMapper objectMapper;

    public RabbitDomainEventPublisher(
            final EventRouting routing, final RabbitEventSender sender, final ObjectMapper objectMapper) {
        this.routing = Objects.requireNonNull(routing, "'routing' should not be null");
        this.sender = Objects.requireNonNull(sender, "'sender' should not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "'objectMapper' should not be null");
    }

    @Override
    public void publishEvent(final DomainEvent event) {
        this.sender.send(this.routing.routingKeyOf(event), this.objectMapper.writeValueAsString(event));
    }
}
