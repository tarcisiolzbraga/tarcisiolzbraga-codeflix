package com.tarcisiolzbraga.codeflix.admin.domain.validation;

import java.util.List;

public interface ValidationHandler {

    ValidationHandler append(ValidationError error);

    ValidationHandler append(ValidationHandler handler);

    List<ValidationError> getErrors();

    default boolean hasError() {
        return !getErrors().isEmpty();
    }

    default ValidationError firstError() {
        return hasError() ? getErrors().getFirst() : null;
    }
}
