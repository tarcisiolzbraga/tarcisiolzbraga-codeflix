package com.tarcisiolzbraga.codeflix.admin.domain.events;

// Quem leva o evento para fora. O domínio só conhece esta interface; a fila é assunto da
// infrastructure, como acontece com os gateways.
@FunctionalInterface
public interface DomainEventPublisher {

    void publishEvent(DomainEvent event);
}
