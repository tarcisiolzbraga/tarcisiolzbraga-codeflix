package com.tarcisiolzbraga.codeflix.admin.domain.castmember;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CastMemberIDTest {

    @Test
    void givenSameValue_whenCompareIDs_thenAreEqual() {
        final var value = "123";
        final var first = CastMemberID.from(value);
        final var second = CastMemberID.from(value);

        final var actualEquals = first.equals(second);

        assertTrue(actualEquals);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void givenDifferentValues_whenCompareIDs_thenAreNotEqual() {
        final var first = CastMemberID.from("123");
        final var second = CastMemberID.from("456");

        final var actualEquals = first.equals(second);

        assertFalse(actualEquals);
    }

    @Test
    void givenUppercaseUUID_whenCallFrom_thenStoreLowercaseValue() {
        final var uuid = UUID.fromString("A1B2C3D4-0000-0000-0000-00000000000F");

        final var actualID = CastMemberID.from(uuid);

        assertEquals("a1b2c3d4-0000-0000-0000-00000000000f", actualID.getValue());
    }

    @Test
    void givenNullValue_whenCallFrom_thenThrowNullPointerException() {
        final String value = null;

        final var actualException = assertThrows(NullPointerException.class, () -> CastMemberID.from(value));

        assertEquals("'value' should not be null", actualException.getMessage());
    }

    @Test
    void givenNoValue_whenCallUnique_thenGenerateLowercaseUUID() {
        final var actualID = CastMemberID.unique();

        assertEquals(actualID.getValue().toLowerCase(), actualID.getValue());
        assertEquals(36, actualID.getValue().length());
    }
}
