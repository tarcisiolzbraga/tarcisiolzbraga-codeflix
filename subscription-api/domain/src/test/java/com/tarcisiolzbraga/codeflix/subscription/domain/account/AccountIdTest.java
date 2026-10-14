package com.tarcisiolzbraga.codeflix.subscription.domain.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountIdTest {

    @Test
    void givenNothing_whenAsksForAUniqueId_thenEachCallGivesADifferentOne() {
        final var one = AccountId.unique();

        final var actual = AccountId.unique();

        assertNotEquals(one, actual);
    }

    @Test
    void givenValidText_whenCreatesFromIt_thenHoldsTheSameValue() {
        final var expected = UUID.randomUUID();

        final var actual = AccountId.from(expected.toString());

        assertEquals(expected, actual.value());
    }

    @Test
    void givenTextThatIsNotUuid_whenCreatesFromIt_thenThrowsDomainException() {
        final var invalid = "conta-1";

        final var actual = assertThrows(DomainException.class, () -> AccountId.from(invalid));

        assertEquals("'conta-1' is not a valid AccountId", actual.getMessage());
    }

    @Test
    void givenNullValue_whenCreates_thenThrowsDomainException() {
        final UUID value = null;

        final var actual = assertThrows(DomainException.class, () -> new AccountId(value));

        assertEquals("'accountId' should not be null", actual.getMessage());
    }
}
