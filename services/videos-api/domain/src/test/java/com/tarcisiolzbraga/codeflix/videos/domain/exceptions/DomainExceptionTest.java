package com.tarcisiolzbraga.codeflix.videos.domain.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationError;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainExceptionTest {

    private static final String FIRST_MESSAGE = "'name' should not be empty";
    private static final String SECOND_MESSAGE = "'id' should not be empty";

    @Test
    void givenAnError_whenCallWith_thenTakeItsMessageAsTheExceptionMessage() {
        final var error = new ValidationError(FIRST_MESSAGE);

        final var actualException = DomainException.with(error);

        assertEquals(FIRST_MESSAGE, actualException.getMessage());
        assertEquals(List.of(error), actualException.getErrors());
    }

    @Test
    void givenManyErrors_whenCallWith_thenKeepAllAndUseTheFirstAsMessage() {
        final var errors = List.of(new ValidationError(FIRST_MESSAGE), new ValidationError(SECOND_MESSAGE));

        final var actualException = DomainException.with(errors);

        assertEquals(FIRST_MESSAGE, actualException.getMessage());
        assertEquals(2, actualException.getErrors().size());
    }

    @Test
    void givenNoError_whenCallWith_thenHaveAnEmptyMessage() {
        final List<ValidationError> errors = List.of();

        final var actualException = DomainException.with(errors);

        assertEquals("", actualException.getMessage());
        assertTrue(actualException.getErrors().isEmpty());
    }

    @Test
    void givenAMutableList_whenCallWith_thenCopyItSoLaterChangesDoNotLeakIn() {
        final var errors = new ArrayList<ValidationError>();
        errors.add(new ValidationError(FIRST_MESSAGE));
        final var actualException = DomainException.with(errors);

        errors.add(new ValidationError(SECOND_MESSAGE));

        assertEquals(1, actualException.getErrors().size());
    }

    @Test
    void givenADomainException_whenCallFillInStackTrace_thenHaveNoStackTrace() {
        final var actualException = DomainException.with(new ValidationError(FIRST_MESSAGE));

        final var actualStackTrace = actualException.getStackTrace();

        assertEquals(0, actualStackTrace.length);
    }
}
