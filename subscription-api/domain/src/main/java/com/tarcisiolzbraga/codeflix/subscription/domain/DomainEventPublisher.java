package com.tarcisiolzbraga.codeflix.subscription.domain;

// Quem recebe o que o agregado acumulou. A implementação mora na infrastructure e grava na fila de
// saída, na mesma transação do agregado; o domínio não sabe que existe tabela, Kafka ou Debezium.
@FunctionalInterface
public interface DomainEventPublisher {

    void publishEvent(DomainEvent event);
}
