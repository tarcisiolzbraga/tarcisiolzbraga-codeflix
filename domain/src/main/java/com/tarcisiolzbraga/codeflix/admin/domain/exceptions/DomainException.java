package com.tarcisiolzbraga.codeflix.admin.domain.exceptions;

import com.tarcisiolzbraga.codeflix.admin.domain.validation.Error;
import java.util.List;

public class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // transient: the exception is handled in-process (turned into an HTTP response), never serialized.
    private final transient List<Error> errors;

    private DomainException(final String message, final List<Error> errors) {
        super(message, null, true, false);
        this.errors = errors;
    }

    public static DomainException with(final Error error) {
        return with(List.of(error));
    }

    public static DomainException with(final List<Error> errors) {
        final var message = errors.isEmpty() ? "" : errors.getFirst().message();
        return new DomainException(message, List.copyOf(errors));
    }

    public List<Error> getErrors() {
        return this.errors;
    }
}
