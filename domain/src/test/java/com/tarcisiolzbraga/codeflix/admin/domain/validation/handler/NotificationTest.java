package com.tarcisiolzbraga.codeflix.admin.domain.validation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import org.junit.jupiter.api.Test;

class NotificationTest {

    @Test
    void givenNoError_whenCallHasError_thenReturnFalse() {
        final var notification = Notification.create();

        final var actualHasError = notification.hasError();

        assertFalse(actualHasError);
        assertTrue(notification.getErrors().isEmpty());
    }

    @Test
    void givenNoError_whenCallFirstError_thenReturnEmpty() {
        final var notification = Notification.create();

        final var actualFirstError = notification.firstError();

        assertTrue(actualFirstError.isEmpty());
    }

    @Test
    void givenTwoErrors_whenCallAppend_thenAccumulateBothInOrder() {
        final var notification = Notification.create();

        notification.append(new ValidationError("primeiro erro"))
                .append(new ValidationError("segundo erro"));

        assertTrue(notification.hasError());
        assertEquals(2, notification.getErrors().size());
        assertEquals("primeiro erro", notification.firstError().orElseThrow().message());
        assertEquals("segundo erro", notification.getErrors().get(1).message());
    }

    @Test
    void givenOtherHandlerWithError_whenCallAppend_thenCopyItsErrors() {
        final var notification = Notification.create();
        final var other = Notification.create().append(new ValidationError("erro externo"));

        notification.append(other);

        assertEquals(1, notification.getErrors().size());
        assertEquals("erro externo", notification.firstError().orElseThrow().message());
    }
}
