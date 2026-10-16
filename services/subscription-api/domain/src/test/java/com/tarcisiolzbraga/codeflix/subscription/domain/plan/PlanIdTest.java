package com.tarcisiolzbraga.codeflix.subscription.domain.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlanIdTest {

    @Test
    void givenNothing_whenAsksForAUniqueId_thenEachCallGivesADifferentOne() {
        final var one = PlanId.unique();

        final var actual = PlanId.unique();

        assertNotEquals(one, actual);
    }

    @Test
    void givenValidText_whenCreatesFromIt_thenHoldsTheSameValue() {
        final var expected = UUID.randomUUID();

        final var actual = PlanId.from(expected.toString());

        assertEquals(expected, actual.value());
    }

    @Test
    void givenAnId_whenAsksForItsTextForm_thenReturnsTheCanonicalForm() {
        final var value = UUID.randomUUID();

        final var actual = new PlanId(value).getValue();

        assertEquals(value.toString(), actual);
    }

    @Test
    void givenTextThatIsNotUuid_whenCreatesFromIt_thenThrowsDomainException() {
        final var invalid = "7";

        final var actual = assertThrows(DomainException.class, () -> PlanId.from(invalid));

        assertEquals("'7' is not a valid PlanId", actual.getMessage());
    }

    @Test
    void givenNullText_whenCreatesFromIt_thenThrowsDomainException() {
        final String invalid = null;

        final var actual = assertThrows(DomainException.class, () -> PlanId.from(invalid));

        assertEquals("'null' is not a valid PlanId", actual.getMessage());
    }

    @Test
    void givenNullValue_whenCreates_thenThrowsDomainException() {
        final UUID value = null;

        final var actual = assertThrows(DomainException.class, () -> new PlanId(value));

        assertEquals("'planId' should not be null", actual.getMessage());
    }
}
