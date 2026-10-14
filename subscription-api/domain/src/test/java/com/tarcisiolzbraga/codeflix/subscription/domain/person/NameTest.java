package com.tarcisiolzbraga.codeflix.subscription.domain.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import org.junit.jupiter.api.Test;

class NameTest {

    private static final String FIRST = "Tarcisio";
    private static final String LAST = "Braga";

    @Test
    void givenFirstAndLast_whenCreates_thenHoldsBoth() {
        final var actual = new Name(FIRST, LAST);

        final var fullname = actual.fullname();

        assertEquals("Tarcisio Braga", fullname);
    }

    @Test
    void givenBlankFirstname_whenCreates_thenThrowsDomainException() {
        final var first = "  ";

        final var actual = assertThrows(DomainException.class, () -> new Name(first, LAST));

        assertEquals("'firstname' should not be empty", actual.getMessage());
    }

    @Test
    void givenBlankLastname_whenCreates_thenThrowsDomainException() {
        final var last = "";

        final var actual = assertThrows(DomainException.class, () -> new Name(FIRST, last));

        assertEquals("'lastname' should not be empty", actual.getMessage());
    }

    @Test
    void givenAFirstnameLongerThanTheColumn_whenCreates_thenThrowsDomainException() {
        final var first = "a".repeat(256);

        final var actual = assertThrows(DomainException.class, () -> new Name(first, LAST));

        assertEquals("'firstname' should not have more than 255 characters", actual.getMessage());
    }

    @Test
    void givenALastnameLongerThanTheColumn_whenCreates_thenThrowsDomainException() {
        final var last = "a".repeat(256);

        final var actual = assertThrows(DomainException.class, () -> new Name(FIRST, last));

        assertEquals("'lastname' should not have more than 255 characters", actual.getMessage());
    }
}
