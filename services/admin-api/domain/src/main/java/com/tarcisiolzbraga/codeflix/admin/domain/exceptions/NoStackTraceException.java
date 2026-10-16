package com.tarcisiolzbraga.codeflix.admin.domain.exceptions;

public class NoStackTraceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoStackTraceException(final String message) {
        this(message, null);
    }

    public NoStackTraceException(final String message, final Throwable cause) {
        super(message, cause, true, false);
    }
}
