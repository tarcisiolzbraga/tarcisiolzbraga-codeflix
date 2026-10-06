package com.tarcisiolzbraga.codeflix.admin.domain.validation.handler;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import java.util.List;

public class ThrowsValidationHandler implements ValidationHandler {

    @Override
    public ValidationHandler append(final ValidationError error) {
        throw DomainException.with(error);
    }

    @Override
    public ValidationHandler append(final ValidationHandler handler) {
        throw DomainException.with(handler.getErrors());
    }

    @Override
    public List<ValidationError> getErrors() {
        return List.of();
    }
}
