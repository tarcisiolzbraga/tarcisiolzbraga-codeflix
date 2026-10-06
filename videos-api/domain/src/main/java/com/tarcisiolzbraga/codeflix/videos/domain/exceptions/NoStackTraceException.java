package com.tarcisiolzbraga.codeflix.videos.domain.exceptions;

// Erro de negócio não precisa de stack trace: ele vira resposta HTTP, não investigação de bug, e
// montar a pilha é o que custa caro numa exceção.
public class NoStackTraceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoStackTraceException(final String message) {
        this(message, null);
    }

    public NoStackTraceException(final String message, final Throwable cause) {
        super(message, cause, true, false);
    }
}
