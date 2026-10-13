package com.tarcisiolzbraga.codeflix.subscription.domain.exceptions;

import com.tarcisiolzbraga.codeflix.subscription.domain.Identifier;

// Uma mensagem, não uma lista de erros como na admin-api: aqui o agregado se valida pelo
// AssertionConcern e recusa no primeiro problema, então nunca há um segundo erro para acumular.
public class DomainException extends NoStackTraceException {

    private static final long serialVersionUID = 1L;

    protected DomainException(final String message) {
        super(message);
    }

    public static DomainException with(final String message) {
        return new DomainException(message);
    }

    public static DomainException notFound(final Class<?> type, final Identifier id) {
        return new DomainException("%s with id %s was not found".formatted(type.getSimpleName(), id.getValue()));
    }
}
