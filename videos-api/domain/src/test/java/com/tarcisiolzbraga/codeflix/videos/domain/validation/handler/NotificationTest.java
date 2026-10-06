package com.tarcisiolzbraga.codeflix.videos.domain.validation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationError;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationTest {

    private static final String FIRST_MESSAGE = "primeiro erro";
    private static final String SECOND_MESSAGE = "segundo erro";

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
    void givenAnError_whenCallCreate_thenStartAlreadyWithIt() {
        final var error = new ValidationError(FIRST_MESSAGE);

        final var actualNotification = Notification.create(error);

        assertTrue(actualNotification.hasError());
        assertEquals(List.of(error), actualNotification.getErrors());
    }

    @Test
    void givenTwoErrors_whenCallAppend_thenAccumulateBothInOrder() {
        final var notification = Notification.create();

        notification.append(new ValidationError(FIRST_MESSAGE)).append(new ValidationError(SECOND_MESSAGE));

        assertEquals(2, notification.getErrors().size());
        assertEquals(FIRST_MESSAGE, notification.firstError().orElseThrow().message());
        assertEquals(SECOND_MESSAGE, notification.getErrors().get(1).message());
    }

    @Test
    void givenOtherHandlerWithError_whenCallAppend_thenCopyItsErrors() {
        final var notification = Notification.create();
        final var other = Notification.create(new ValidationError(FIRST_MESSAGE));

        notification.append(other);

        assertEquals(1, notification.getErrors().size());
        assertEquals(FIRST_MESSAGE, notification.firstError().orElseThrow().message());
    }

    @Test
    void givenANotification_whenChangeTheReturnedErrors_thenRefuseTheChange() {
        final var notification = Notification.create(new ValidationError(FIRST_MESSAGE));

        final var actualErrors = notification.getErrors();

        assertThrows(
                UnsupportedOperationException.class,
                () -> actualErrors.add(new ValidationError(SECOND_MESSAGE)));
        assertEquals(1, notification.getErrors().size());
    }
}
