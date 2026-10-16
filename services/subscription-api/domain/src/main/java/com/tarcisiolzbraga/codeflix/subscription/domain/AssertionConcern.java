package com.tarcisiolzbraga.codeflix.subscription.domain;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;

// A validação deste serviço: o próprio agregado recusa o valor inválido no momento em que o recebe,
// e não existe objeto mal formado. É o caminho do curso, e diverge da admin-api, que acumula os
// erros numa Notification e devolve todos de uma vez.
//
// A troca é consciente: aqui o cliente recebe um erro por vez, e em troca não há validador separado,
// handler nem estado de validação para carregar. Para agregados pequenos como estes, paga.
public interface AssertionConcern {

    default <T> T assertArgumentNotNull(final T value, final String message) {
        if (value == null) {
            throw DomainException.with(message);
        }
        return value;
    }

    default String assertArgumentNotEmpty(final String value, final String message) {
        if (value == null || value.isBlank()) {
            throw DomainException.with(message);
        }
        return value;
    }

    default String assertArgumentLength(final String value, final int length, final String message) {
        if (value == null || value.length() != length) {
            throw DomainException.with(message);
        }
        return value;
    }

    // Devolve o valor, não a mensagem: a referência devolve `aMessage` aqui, e quem usasse o retorno
    // guardaria o texto do erro no lugar do dado. Ausência não tem comprimento, então null passa —
    // quem exige presença usa o assertArgumentNotEmpty junto.
    default String assertArgumentMaxLength(final String value, final int length, final String message) {
        if (value != null && value.length() > length) {
            throw DomainException.with(message);
        }
        return value;
    }

    default void assertConditionTrue(final Boolean condition, final String message) {
        if (!Boolean.TRUE.equals(condition)) {
            throw DomainException.with(message);
        }
    }
}
