package com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka.models.connect;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

// As operações que o Debezium põe no campo "op".
//
// READ existe e não é detalhe: o snapshot inicial publica cada linha já existente com "r", não com
// "c". Sem declará-lo, a desserialização devolveria nulo para toda a carga inicial.
//
// TRUNCATE também é real, e é o caso perigoso: ele não traz before nem after, então tratá-lo como
// "não é delete" faria o consumidor ler o id de um after inexistente.
public enum Operation {
    READ("r"),
    CREATE("c"),
    UPDATE("u"),
    DELETE("d"),
    TRUNCATE("t");

    private final String code;

    Operation(final String code) {
        this.code = code;
    }

    // Devolve nulo no código desconhecido, em vez de estourar: uma versão nova do Debezium pode
    // trazer uma operação que não existe aqui, e derrubar a desserialização por isso pararia o
    // consumo da partição inteira. Quem embrulha esse nulo num Optional é o ValuePayload.
    //
    // O creator precisa devolver o próprio enum: devolvendo Optional, o Jackson o ignora em
    // silêncio e cai no desserializador padrão de enum, que recusa o valor desconhecido.
    @JsonCreator
    public static Operation of(final String value) {
        return Arrays.stream(values())
                .filter(it -> it.code.equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }

    @JsonValue
    public String code() {
        return this.code;
    }

    public boolean isDelete() {
        return this == DELETE;
    }

    // O que traz a linha em "after": o estado novo do registro.
    public boolean carriesNewState() {
        return this == READ || this == CREATE || this == UPDATE;
    }
}
