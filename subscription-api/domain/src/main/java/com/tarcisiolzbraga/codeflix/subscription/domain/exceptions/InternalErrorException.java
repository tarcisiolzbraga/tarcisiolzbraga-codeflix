package com.tarcisiolzbraga.codeflix.subscription.domain.exceptions;

// Falha nossa, não do cliente: o que chega aqui vira 500, e por isso guarda a causa — diferente da
// DomainException, que é entrada recusada e vira 422.
public class InternalErrorException extends NoStackTraceException {

    private static final long serialVersionUID = 1L;

    private InternalErrorException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public static InternalErrorException with(final String message) {
        return new InternalErrorException(message, null);
    }

    public static InternalErrorException with(final String message, final Throwable cause) {
        return new InternalErrorException(message, cause);
    }
}
