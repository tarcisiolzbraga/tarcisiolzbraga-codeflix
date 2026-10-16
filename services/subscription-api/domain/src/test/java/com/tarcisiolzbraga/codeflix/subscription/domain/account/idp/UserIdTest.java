package com.tarcisiolzbraga.codeflix.subscription.domain.account.idp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import org.junit.jupiter.api.Test;

class UserIdTest {

    // Guarda texto, e não UUID como os ids daqui: este id é do Keycloak, não nosso. Ele hoje usa
    // UUID, mas exigir esse formato seria impor a um sistema de fora uma regra que não é nossa.
    @Test
    void givenAnIdentifierFromTheProvider_whenCreates_thenHoldsItAsText() {
        final var value = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";

        final var actual = new UserId(value);

        assertEquals(value, actual.value());
    }

    @Test
    void givenAnIdentifierThatIsNotUuid_whenCreates_thenItIsStillAccepted() {
        final var value = "auth0|507f1f77bcf86cd799439011";

        final var actual = new UserId(value);

        assertEquals(value, actual.value());
    }

    @Test
    void givenBlank_whenCreates_thenThrowsDomainException() {
        final var value = "  ";

        final var actual = assertThrows(DomainException.class, () -> new UserId(value));

        assertEquals("'userId' should not be empty", actual.getMessage());
    }

    @Test
    void givenNull_whenCreates_thenThrowsDomainException() {
        final String value = null;

        final var actual = assertThrows(DomainException.class, () -> new UserId(value));

        assertEquals("'userId' should not be empty", actual.getMessage());
    }
}
