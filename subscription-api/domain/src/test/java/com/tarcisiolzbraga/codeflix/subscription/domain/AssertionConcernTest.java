package com.tarcisiolzbraga.codeflix.subscription.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import org.junit.jupiter.api.Test;

class AssertionConcernTest {

    private static final String MESSAGE = "'field' is invalid";

    private final AssertionConcern subject = new AssertionConcern() {};

    @Test
    void givenValueNotNull_whenAssertsNotNull_thenReturnsTheValueItself() {
        final var value = "codeflix";

        final var actual = subject.assertArgumentNotNull(value, MESSAGE);

        assertSame(value, actual);
    }

    @Test
    void givenNull_whenAssertsNotNull_thenThrowsWithTheGivenMessage() {
        final String value = null;

        final var actual = assertThrows(DomainException.class, () -> subject.assertArgumentNotNull(value, MESSAGE));

        assertEquals(MESSAGE, actual.getMessage());
    }

    @Test
    void givenBlankText_whenAssertsNotEmpty_thenThrowsWithTheGivenMessage() {
        final var value = "   ";

        final var actual = assertThrows(DomainException.class, () -> subject.assertArgumentNotEmpty(value, MESSAGE));

        assertEquals(MESSAGE, actual.getMessage());
    }

    @Test
    void givenTextOfAnotherLength_whenAssertsLength_thenThrowsWithTheGivenMessage() {
        final var value = "123";

        final var actual = assertThrows(DomainException.class, () -> subject.assertArgumentLength(value, 11, MESSAGE));

        assertEquals(MESSAGE, actual.getMessage());
    }

    @Test
    void givenTextLongerThanTheMaximum_whenAssertsMaxLength_thenThrowsWithTheGivenMessage() {
        final var value = "123456";

        final var actual = assertThrows(DomainException.class, () -> subject.assertArgumentMaxLength(value, 5, MESSAGE));

        assertEquals(MESSAGE, actual.getMessage());
    }

    @Test
    void givenTextWithinTheMaximum_whenAssertsMaxLength_thenReturnsTheValueAndNotTheMessage() {
        final var value = "12345";

        final var actual = subject.assertArgumentMaxLength(value, 5, MESSAGE);

        assertSame(value, actual);
    }

    @Test
    void givenNullText_whenAssertsMaxLength_thenReturnsNullBecauseAbsenceIsNotLength() {
        final String value = null;

        final var actual = subject.assertArgumentMaxLength(value, 5, MESSAGE);

        assertSame(value, actual);
    }

    @Test
    void givenFalseCondition_whenAssertsConditionTrue_thenThrowsWithTheGivenMessage() {
        final var condition = false;

        final var actual = assertThrows(DomainException.class, () -> subject.assertConditionTrue(condition, MESSAGE));

        assertEquals(MESSAGE, actual.getMessage());
    }
}
