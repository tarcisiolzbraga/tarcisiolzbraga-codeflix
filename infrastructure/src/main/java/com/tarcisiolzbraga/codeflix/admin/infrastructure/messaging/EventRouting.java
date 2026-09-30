package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging;

import com.tarcisiolzbraga.codeflix.admin.domain.events.DomainEvent;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaCreated;
import java.util.Objects;

// A outra metade: decide para onde cada evento vai. Evento sem destino é erro de programação, não
// de dados, então falha alto em vez de sumir com a mensagem.
public class EventRouting {

    private static final String UNKNOWN_EVENT_MESSAGE = "no routing key configured for %s";

    private final String videoCreatedRoutingKey;

    public EventRouting(final String videoCreatedRoutingKey) {
        this.videoCreatedRoutingKey =
                Objects.requireNonNull(videoCreatedRoutingKey, "'videoCreatedRoutingKey' should not be null");
    }

    public String routingKeyOf(final DomainEvent event) {
        if (event instanceof VideoMediaCreated) {
            return this.videoCreatedRoutingKey;
        }
        throw new IllegalArgumentException(UNKNOWN_EVENT_MESSAGE.formatted(event.getClass().getSimpleName()));
    }
}
