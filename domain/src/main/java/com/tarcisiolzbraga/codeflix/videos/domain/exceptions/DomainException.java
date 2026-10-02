package com.tarcisiolzbraga.codeflix.videos.domain.exceptions;

import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationError;
import java.util.List;

public class DomainException extends NoStackTraceException {

    private static final long serialVersionUID = 1L;

    // transient: a exceção é tratada no processo (virando resposta HTTP), nunca serializada.
    private final transient List<ValidationError> errors;

    protected DomainException(final String message, final List<ValidationError> errors) {
        super(message);
        this.errors = errors;
    }

    public static DomainException with(final ValidationError error) {
        return with(List.of(error));
    }

    public static DomainException with(final List<ValidationError> errors) {
        final var message = errors.isEmpty() ? "" : errors.getFirst().message();
        return new DomainException(message, List.copyOf(errors));
    }

    public List<ValidationError> getErrors() {
        return this.errors;
    }
}
