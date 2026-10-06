package com.tarcisiolzbraga.codeflix.videos.domain.castmember;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CastMemberIDTest {

    private static final String EXPECTED_VALUE = "7c1e3d4a-9b2f-4c80-8d61-a2b3c4d5e6f7";

    @Test
    void givenAValue_whenCallFrom_thenHoldIt() {
        final var actualId = CastMemberID.from(EXPECTED_VALUE);

        assertEquals(EXPECTED_VALUE, actualId.getValue());
        assertEquals(EXPECTED_VALUE, actualId.value());
    }

    @Test
    void givenNullValue_whenCallFrom_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> CastMemberID.from(null));

        assertEquals("'value' should not be null", actualException.getMessage());
    }

    @Test
    void givenTwoIdsWithTheSameValue_whenCompare_thenBeEqual() {
        final var one = CastMemberID.from(EXPECTED_VALUE);
        final var other = CastMemberID.from(EXPECTED_VALUE);

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }
}
