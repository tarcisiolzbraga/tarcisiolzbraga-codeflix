package com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka.models.connect;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;

// O miolo da mensagem do Debezium: a linha antes e depois da mudança, mais a operação.
//
// before vem nulo na criação e no snapshot; after vem nulo na remoção. Por isso os dois saem como
// Optional, em vez de deixar o chamador adivinhar qual dos dois está preenchido.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ValuePayload<T>(
        @JsonProperty("before") T before,
        @JsonProperty("after") T after,
        @JsonProperty("source") Source source,
        @JsonProperty("op") Operation op) {

    // Vazio quando o Debezium mandou uma operação que esta versão não conhece.
    public Optional<Operation> operation() {
        return Optional.ofNullable(this.op);
    }

    public Optional<T> beforeState() {
        return Optional.ofNullable(this.before);
    }

    public Optional<T> afterState() {
        return Optional.ofNullable(this.after);
    }
}
