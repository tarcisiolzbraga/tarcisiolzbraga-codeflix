package com.tarcisiolzbraga.codeflix.admin.infrastructure.api;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import java.util.List;

public record ApiError(String message, List<String> errors) {

    public ApiError {
        errors = List.copyOf(errors);
    }

    public static ApiError from(final DomainException exception) {
        return new ApiError(exception.getMessage(), messagesOf(exception.getErrors()));
    }

    public static ApiError from(final Notification notification) {
        final var messages = messagesOf(notification.getErrors());
        return new ApiError(messages.isEmpty() ? "" : messages.getFirst(), messages);
    }

    private static List<String> messagesOf(final List<ValidationError> errors) {
        return errors.stream().map(ValidationError::message).toList();
    }
}
