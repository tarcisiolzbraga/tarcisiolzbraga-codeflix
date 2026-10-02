package com.tarcisiolzbraga.codeflix.videos.domain.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CategoryIDTest {

    private static final String EXPECTED_VALUE = "3f2b1a9c-5d6e-4f70-8a91-b2c3d4e5f607";

    @Test
    void givenAValue_whenCallFrom_thenHoldIt() {
        final var actualId = CategoryID.from(EXPECTED_VALUE);

        assertEquals(EXPECTED_VALUE, actualId.getValue());
        assertEquals(EXPECTED_VALUE, actualId.value());
    }

    @Test
    void givenNullValue_whenCallFrom_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> CategoryID.from(null));

        assertEquals("'value' should not be null", actualException.getMessage());
    }

    @Test
    void givenTwoIdsWithTheSameValue_whenCompare_thenBeEqual() {
        final var one = CategoryID.from(EXPECTED_VALUE);
        final var other = CategoryID.from(EXPECTED_VALUE);

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }
}
