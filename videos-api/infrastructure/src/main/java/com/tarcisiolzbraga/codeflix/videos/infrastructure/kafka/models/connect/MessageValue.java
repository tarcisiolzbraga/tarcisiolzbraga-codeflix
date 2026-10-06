package com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka.models.connect;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// O envelope do Kafka Connect. Com value.converter.schemas.enable ligado, cada mensagem vem como
// { "schema": { ... }, "payload": { ... } }, e só o payload interessa — o schema é a descrição dos
// tipos, que ocupa a maior parte dos seis quilobytes de cada mensagem.
//
// ignoreUnknown de propósito: este contrato não é nosso. Campo novo numa versão do Debezium não
// pode derrubar o consumidor, ao contrário dos nossos próprios DTOs, em que campo a mais é erro.
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageValue<T>(@JsonProperty("payload") ValuePayload<T> payload) {
}
