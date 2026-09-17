package com.tarcisiolzbraga.codeflix.admin.domain.validation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.Error;
import org.junit.jupiter.api.Test;

class ThrowsValidationHandlerTest {

    @Test
    void givenAnError_whenCallAppend_thenThrowDomainExceptionWithIt() {
        final var handler = new ThrowsValidationHandler();
        final var expectedMessage = "erro de validação";

        final var actualException = assertThrows(
                DomainException.class, () -> handler.append(new Error(expectedMessage)));

        assertEquals(1, actualException.getErrors().size());
        assertEquals(expectedMessage, actualException.getErrors().getFirst().message());
        assertEquals(expectedMessage, actualException.getMessage());
    }

    @Test
    void givenHandlerWithErrors_whenCallAppend_thenThrowDomainExceptionWithAllOfThem() {
        final var handler = new ThrowsValidationHandler();
        final var other = Notification.create()
                .append(new Error("primeiro erro"))
                .append(new Error("segundo erro"));

        final var actualException = assertThrows(DomainException.class, () -> handler.append(other));

        assertEquals(2, actualException.getErrors().size());
    }
}
