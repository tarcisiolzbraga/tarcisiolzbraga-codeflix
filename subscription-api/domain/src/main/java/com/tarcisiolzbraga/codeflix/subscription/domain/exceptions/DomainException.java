package com.tarcisiolzbraga.codeflix.subscription.domain.exceptions;

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
}
