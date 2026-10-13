package com.tarcisiolzbraga.codeflix.subscription.domain;

import java.time.Instant;

// O que a fila de saída precisa saber de qualquer evento para gravá-lo: quando aconteceu e sobre
// quem. O conteúdo é de cada evento, e vai para a coluna JSON.
public interface DomainEvent extends AssertionConcern {

    Instant occurredOn();

    String aggregateId();

    String aggregateType();
}
