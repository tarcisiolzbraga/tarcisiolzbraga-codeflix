package com.tarcisiolzbraga.codeflix.subscription.domain.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import org.junit.jupiter.api.Test;

class AddressTest {

    private static final String ZIPCODE = "01310930";
    private static final String NUMBER = "1578";
    private static final String COMPLEMENT = "apto 42";
    private static final String COUNTRY = "BR";

    @Test
    void givenTheFullAddress_whenCreates_thenHoldsIt() {
        final var actual = new Address(ZIPCODE, NUMBER, COMPLEMENT, COUNTRY);

        final var zipcode = actual.zipcode();

        assertEquals(ZIPCODE, zipcode);
    }

    // O complemento é o único opcional: nem todo endereço tem um.
    @Test
    void givenNoComplement_whenCreates_thenItIsAccepted() {
        final var actual = new Address(ZIPCODE, NUMBER, null, COUNTRY);

        final var complement = actual.complement();

        assertNull(complement);
    }

    @Test
    void givenBlankZipcode_whenCreates_thenThrowsDomainException() {
        final var zipcode = " ";

        final var actual =
                assertThrows(DomainException.class, () -> new Address(zipcode, NUMBER, COMPLEMENT, COUNTRY));

        assertEquals("'zipcode' should not be empty", actual.getMessage());
    }

    @Test
    void givenBlankNumber_whenCreates_thenThrowsDomainException() {
        final var number = "";

        final var actual =
                assertThrows(DomainException.class, () -> new Address(ZIPCODE, number, COMPLEMENT, COUNTRY));

        assertEquals("'number' should not be empty", actual.getMessage());
    }

    @Test
    void givenBlankCountry_whenCreates_thenThrowsDomainException() {
        final String country = null;

        final var actual =
                assertThrows(DomainException.class, () -> new Address(ZIPCODE, NUMBER, COMPLEMENT, country));

        assertEquals("'country' should not be empty", actual.getMessage());
    }

    @Test
    void givenAComplementLongerThanTheColumn_whenCreates_thenThrowsDomainException() {
        final var complement = "a".repeat(256);

        final var actual =
                assertThrows(DomainException.class, () -> new Address(ZIPCODE, NUMBER, complement, COUNTRY));

        assertEquals("'complement' should not have more than 255 characters", actual.getMessage());
    }
}
