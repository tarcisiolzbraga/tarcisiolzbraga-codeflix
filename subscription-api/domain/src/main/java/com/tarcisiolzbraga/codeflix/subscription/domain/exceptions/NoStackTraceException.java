package com.tarcisiolzbraga.codeflix.subscription.domain.exceptions;

// Erro de domínio é resposta esperada, não defeito: a pilha não serve para nada e custa caro de
// montar. Sem ela, recusar uma entrada inválida fica barato mesmo sob carga.
public class NoStackTraceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoStackTraceException(final String message) {
        this(message, null);
    }

    public NoStackTraceException(final String message, final Throwable cause) {
        super(message, cause, true, false);
    }
}
