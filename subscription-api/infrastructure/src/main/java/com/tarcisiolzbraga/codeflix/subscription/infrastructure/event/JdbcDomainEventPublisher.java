package com.tarcisiolzbraga.codeflix.subscription.infrastructure.event;

import com.tarcisiolzbraga.codeflix.subscription.domain.DomainEvent;
import com.tarcisiolzbraga.codeflix.subscription.domain.DomainEventPublisher;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc.DatabaseClient;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

// Grava o evento na fila de saída. Quem chama é o repositório, dentro da transação que gravou o
// agregado — é isso que faz "gravou e avisou" ser atômico.
//
// Não publica em broker nenhum, e é de propósito: daqui para frente é o Debezium lendo o binlog que
// leva o evento adiante. Este serviço não conhece Kafka no lado de escrita.
@Component
public class JdbcDomainEventPublisher implements DomainEventPublisher {

    private static final String INSERT = """
            INSERT INTO event (id, aggregate_type, aggregate_id, event_type, occurred_on, payload)
            VALUES (:id, :aggregateType, :aggregateId, :eventType, :occurredOn, :payload)
            """;

    private final DatabaseClient database;
    private final ObjectMapper mapper;

    public JdbcDomainEventPublisher(final DatabaseClient database, final ObjectMapper mapper) {
        this.database = Objects.requireNonNull(database, "'database' should not be null");
        this.mapper = Objects.requireNonNull(mapper, "'mapper' should not be null");
    }

    @Override
    public void publishEvent(final DomainEvent event) {
        Objects.requireNonNull(event, "'event' should not be null");
        this.database.update(
                INSERT,
                Map.of(
                        "id", UUID.randomUUID().toString(),
                        "aggregateType", event.aggregateType(),
                        "aggregateId", event.aggregateId(),
                        "eventType", eventTypeOf(event),
                        "occurredOn", Timestamp.from(event.occurredOn()),
                        "payload", this.mapper.writeValueAsString(event)));
    }

    // O nome simples da classe, e não o qualificado como na referência: o tipo do evento é contrato
    // com quem consome, e o nome qualificado amarraria esse contrato ao nosso layout de pacotes —
    // mover a classe de pacote viraria mudança incompatível.
    private static String eventTypeOf(final DomainEvent event) {
        return event.getClass().getSimpleName();
    }
}
