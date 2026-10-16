package com.tarcisiolzbraga.codeflix.subscription.infrastructure.kafka.models.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// A linha da fila de saída como o Debezium a entrega.
//
// O consumidor age só com isto, sem ler a linha de volta pelo id como a referência faz: a mensagem
// já traz tudo, e a ida ao banco custaria um round-trip por evento e abriria corrida entre dois
// consumidores lendo a mesma linha não processada.
//
// ignoreUnknown porque este contrato não é nosso: coluna nova na tabela não pode derrubar o
// consumidor, e o `payload` só é lido por quem sabe de qual evento se trata.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventMsg(
        @JsonProperty("id") String id,
        @JsonProperty("aggregate_type") String aggregateType,
        @JsonProperty("aggregate_id") String aggregateId,
        @JsonProperty("event_type") String eventType,
        @JsonProperty("payload") String payload) {}
