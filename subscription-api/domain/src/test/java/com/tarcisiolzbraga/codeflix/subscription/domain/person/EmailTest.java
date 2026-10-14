package com.tarcisiolzbraga.codeflix.subscription.domain.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import org.junit.jupiter.api.Test;

class EmailTest {

    private static final String EMPTY_MESSAGE = "'email' should not be empty";
    private static final String INVALID_MESSAGE = "'email' is invalid";

    @Test
    void givenAnAddress_whenCreates_thenHoldsIt() {
        final var value = "assinante@codeflix.com.br";

        final var actual = new Email(value);

        assertEquals(value, actual.value());
    }

    @Test
    void givenAnAddressWithoutADotInTheDomain_whenCreates_thenItIsAccepted() {
        final var value = "assinante@localhost";

        final var actual = new Email(value);

        assertEquals(value, actual.value());
    }

    @Test
    void givenBlank_whenCreates_thenThrowsDomainException() {
        final var value = "   ";

        final var actual = assertThrows(DomainException.class, () -> new Email(value));

        assertEquals(EMPTY_MESSAGE, actual.getMessage());
    }

    @Test
    void givenNull_whenCreates_thenThrowsDomainException() {
        final String value = null;

        final var actual = assertThrows(DomainException.class, () -> new Email(value));

        assertEquals(EMPTY_MESSAGE, actual.getMessage());
    }

    @Test
    void givenAnAddressWithoutTheAtSign_whenCreates_thenThrowsDomainException() {
        final var value = "assinante.codeflix.com.br";

        final var actual = assertThrows(DomainException.class, () -> new Email(value));

        assertEquals(INVALID_MESSAGE, actual.getMessage());
    }

    @Test
    void givenAnAddressWithTwoAtSigns_whenCreates_thenThrowsDomainException() {
        final var value = "assinante@codeflix@com";

        final var actual = assertThrows(DomainException.class, () -> new Email(value));

        assertEquals(INVALID_MESSAGE, actual.getMessage());
    }

    // A referência aceita isto: o padrão dela é ^(.+)@(\S+)$, e o .+ casa espaço.
    @Test
    void givenAnAddressWithASpaceInTheLocalPart_whenCreates_thenThrowsDomainException() {
        final var value = "assi nante@codeflix.com";

        final var actual = assertThrows(DomainException.class, () -> new Email(value));

        assertEquals(INVALID_MESSAGE, actual.getMessage());
    }

    @Test
    void givenAnAddressWithNothingBeforeTheAtSign_whenCreates_thenThrowsDomainException() {
        final var value = "@codeflix.com";

        final var actual = assertThrows(DomainException.class, () -> new Email(value));

        assertEquals(INVALID_MESSAGE, actual.getMessage());
    }

    @Test
    void givenAnAddressWithNothingAfterTheAtSign_whenCreates_thenThrowsDomainException() {
        final var value = "assinante@";

        final var actual = assertThrows(DomainException.class, () -> new Email(value));

        assertEquals(INVALID_MESSAGE, actual.getMessage());
    }

    // A coluna é VARCHAR(255): o que não caberia nela é recusado aqui, e não pelo banco.
    @Test
    void givenAnAddressLongerThanTheColumn_whenCreates_thenThrowsDomainException() {
        final var value = "a".repeat(250) + "@codeflix.com";

        final var actual = assertThrows(DomainException.class, () -> new Email(value));

        assertEquals("'email' should not have more than 255 characters", actual.getMessage());
    }
}
