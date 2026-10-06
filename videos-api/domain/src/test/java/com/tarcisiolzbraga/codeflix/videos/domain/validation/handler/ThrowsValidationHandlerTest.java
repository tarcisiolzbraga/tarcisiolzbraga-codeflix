package com.tarcisiolzbraga.codeflix.videos.domain.validation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationError;
import org.junit.jupiter.api.Test;

class ThrowsValidationHandlerTest {

    private static final String EXPECTED_MESSAGE = "'name' should not be empty";

    @Test
    void givenAnError_whenCallAppend_thenThrowDomainExceptionWithIt() {
        final var handler = new ThrowsValidationHandler();
        final var error = new ValidationError(EXPECTED_MESSAGE);

        final var actualException = assertThrows(DomainException.class, () -> handler.append(error));

        assertEquals(EXPECTED_MESSAGE, actualException.getMessage());
        assertEquals(1, actualException.getErrors().size());
    }

    @Test
    void givenOtherHandlerWithErrors_whenCallAppend_thenThrowDomainExceptionWithAllOfThem() {
        final var handler = new ThrowsValidationHandler();
        final var other = Notification.create(new ValidationError(EXPECTED_MESSAGE))
                .append(new ValidationError("'id' should not be empty"));

        final var actualException = assertThrows(DomainException.class, () -> handler.append(other));

        assertEquals(EXPECTED_MESSAGE, actualException.getMessage());
        assertEquals(2, actualException.getErrors().size());
    }

    @Test
    void givenAHandler_whenCallGetErrors_thenReturnEmptyBecauseItNeverAccumulates() {
        final var handler = new ThrowsValidationHandler();

        final var actualErrors = handler.getErrors();

        assertTrue(actualErrors.isEmpty());
    }
}
