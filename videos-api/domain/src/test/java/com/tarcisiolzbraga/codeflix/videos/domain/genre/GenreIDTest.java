package com.tarcisiolzbraga.codeflix.videos.domain.genre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GenreIDTest {

    private static final String EXPECTED_VALUE = "5b8d2e1f-3a4c-4d60-9e71-f2a3b4c5d6e7";

    @Test
    void givenAValue_whenCallFrom_thenHoldIt() {
        final var actualId = GenreID.from(EXPECTED_VALUE);

        assertEquals(EXPECTED_VALUE, actualId.getValue());
        assertEquals(EXPECTED_VALUE, actualId.value());
    }

    @Test
    void givenNullValue_whenCallFrom_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> GenreID.from(null));

        assertEquals("'value' should not be null", actualException.getMessage());
    }

    @Test
    void givenTwoIdsWithTheSameValue_whenCompare_thenBeEqual() {
        final var one = GenreID.from(EXPECTED_VALUE);
        final var other = GenreID.from(EXPECTED_VALUE);

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }
}
