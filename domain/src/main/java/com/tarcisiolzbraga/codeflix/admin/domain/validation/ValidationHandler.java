package com.tarcisiolzbraga.codeflix.admin.domain.validation;

import java.util.List;

public interface ValidationHandler {

    ValidationHandler append(Error error);

    ValidationHandler append(ValidationHandler handler);

    List<Error> getErrors();

    default boolean hasError() {
        return !getErrors().isEmpty();
    }

    default Error firstError() {
        return hasError() ? getErrors().getFirst() : null;
    }
}
