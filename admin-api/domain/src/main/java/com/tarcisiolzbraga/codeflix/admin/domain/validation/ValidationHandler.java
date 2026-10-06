package com.tarcisiolzbraga.codeflix.admin.domain.validation;

import java.util.List;
import java.util.Optional;

public interface ValidationHandler {

    ValidationHandler append(ValidationError error);

    ValidationHandler append(ValidationHandler handler);

    List<ValidationError> getErrors();

    default boolean hasError() {
        return !getErrors().isEmpty();
    }

    default Optional<ValidationError> firstError() {
        return getErrors().stream().findFirst();
    }
}
