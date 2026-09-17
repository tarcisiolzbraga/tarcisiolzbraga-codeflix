package com.tarcisiolzbraga.codeflix.admin.domain.validation.handler;

import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import java.util.ArrayList;
import java.util.List;

public class Notification implements ValidationHandler {

    private final List<ValidationError> errors;

    private Notification(final List<ValidationError> errors) {
        this.errors = errors;
    }

    public static Notification create() {
        return new Notification(new ArrayList<>());
    }

    public static Notification create(final ValidationError error) {
        return create().append(error);
    }

    @Override
    public Notification append(final ValidationError error) {
        this.errors.add(error);
        return this;
    }

    @Override
    public Notification append(final ValidationHandler handler) {
        this.errors.addAll(handler.getErrors());
        return this;
    }

    @Override
    public List<ValidationError> getErrors() {
        return List.copyOf(this.errors);
    }
}
