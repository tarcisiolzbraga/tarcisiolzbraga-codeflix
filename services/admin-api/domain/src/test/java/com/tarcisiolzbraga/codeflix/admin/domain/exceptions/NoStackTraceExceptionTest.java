package com.tarcisiolzbraga.codeflix.admin.domain.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class NoStackTraceExceptionTest {

    private static final String EXPECTED_MESSAGE = "algo deu errado";

    @Test
    void givenMessage_whenCreateException_thenHaveNoStackTrace() {
        final var exception = new NoStackTraceException(EXPECTED_MESSAGE);

        final var actualStackTrace = exception.getStackTrace();

        assertEquals(0, actualStackTrace.length);
        assertEquals(EXPECTED_MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void givenCause_whenCreateException_thenKeepItWithoutStackTrace() {
        final var expectedCause = new IllegalStateException("causa");

        final var exception = new NoStackTraceException(EXPECTED_MESSAGE, expectedCause);

        assertEquals(0, exception.getStackTrace().length);
        assertSame(expectedCause, exception.getCause());
    }
}
