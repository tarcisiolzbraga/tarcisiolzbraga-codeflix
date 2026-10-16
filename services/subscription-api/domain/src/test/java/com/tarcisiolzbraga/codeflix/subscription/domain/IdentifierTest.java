package com.tarcisiolzbraga.codeflix.subscription.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentifierTest {

    @Test
    void givenValidText_whenConvertsToUuid_thenReturnsTheSameValue() {
        final var expected = UUID.randomUUID();

        final var actual = Identifier.uuidOf(expected.toString(), StubId.class);

        assertEquals(expected, actual);
    }

    @Test
    void givenUpperCaseText_whenConvertsToUuid_thenNormalizesToTheCanonicalForm() {
        final var expected = UUID.randomUUID();

        final var actual = Identifier.uuidOf(expected.toString().toUpperCase(), StubId.class);

        assertEquals(expected, actual);
    }

    @Test
    void givenTextThatIsNotUuid_whenConvertsToUuid_thenThrowsDomainException() {
        final var invalid = "not-an-uuid";

        final var actual = assertThrows(DomainException.class, () -> Identifier.uuidOf(invalid, StubId.class));

        assertEquals("'not-an-uuid' is not a valid StubId", actual.getMessage());
    }

    @Test
    void givenNull_whenConvertsToUuid_thenThrowsDomainException() {
        final String invalid = null;

        final var actual = assertThrows(DomainException.class, () -> Identifier.uuidOf(invalid, StubId.class));

        assertEquals("'null' is not a valid StubId", actual.getMessage());
    }

    @Test
    void givenAnId_whenAsksForItsTextForm_thenReturnsTheLowerCaseCanonicalForm() {
        final var value = UUID.randomUUID();

        final var actual = new StubId(value).getValue();

        assertEquals(value.toString(), actual);
    }

    private record StubId(UUID value) implements Identifier {}
}
