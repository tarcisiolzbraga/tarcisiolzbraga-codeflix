package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging;

import com.tarcisiolzbraga.codeflix.admin.domain.events.DomainEvent;
import com.tarcisiolzbraga.codeflix.admin.domain.events.DomainEventPublisher;
import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.persistence.OutboxEventJpaEntity;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.persistence.OutboxEventRepository;
import java.util.Objects;
import tools.jackson.databind.ObjectMapper;

// Não fala com o broker: grava a linha e volta. Como quem chama está dentro da transação que salva
// o agregado, ou as duas coisas acontecem ou nenhuma — que é o ponto do padrão. Quem entrega
// depois é o relay.
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final EventRouting routing;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxDomainEventPublisher(
            final EventRouting routing,
            final OutboxEventRepository outboxRepository,
            final ObjectMapper objectMapper) {
        this.routing = Objects.requireNonNull(routing, "'routing' should not be null");
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "'outboxRepository' should not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "'objectMapper' should not be null");
    }

    @Override
    public void publishEvent(final DomainEvent event) {
        this.outboxRepository.save(OutboxEventJpaEntity.pending(
                this.routing.routingKeyOf(event),
                this.objectMapper.writeValueAsString(event),
                InstantUtils.now()));
    }
}
