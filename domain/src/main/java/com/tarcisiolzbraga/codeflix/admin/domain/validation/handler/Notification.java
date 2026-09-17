package com.tarcisiolzbraga.codeflix.admin.domain.validation.handler;

import com.tarcisiolzbraga.codeflix.admin.domain.validation.Error;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import java.util.ArrayList;
import java.util.List;

public class Notification implements ValidationHandler {

    private final List<Error> errors;

    private Notification(final List<Error> errors) {
        this.errors = errors;
    }

    public static Notification create() {
        return new Notification(new ArrayList<>());
    }

    public static Notification create(final Error error) {
        return create().append(error);
    }

    @Override
    public Notification append(final Error error) {
        this.errors.add(error);
        return this;
    }

    @Override
    public Notification append(final ValidationHandler handler) {
        this.errors.addAll(handler.getErrors());
        return this;
    }

    @Override
    public List<Error> getErrors() {
        return List.copyOf(this.errors);
    }
}
