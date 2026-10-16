package com.tarcisiolzbraga.codeflix.admin.domain.castmember;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class CastMemberTypeTest {

    @Test
    void givenTypeName_whenCallOf_thenReturnIt() {
        final var actualType = CastMemberType.of("ACTOR");

        assertEquals(Optional.of(CastMemberType.ACTOR), actualType);
    }

    @Test
    void givenTypeNameInAnotherCase_whenCallOf_thenReturnIt() {
        final var actualType = CastMemberType.of("director");

        assertEquals(Optional.of(CastMemberType.DIRECTOR), actualType);
    }

    @Test
    void givenUnknownValue_whenCallOf_thenReturnEmpty() {
        final var actualType = CastMemberType.of("SINGER");

        assertTrue(actualType.isEmpty());
    }

    @Test
    void givenNullValue_whenCallOf_thenReturnEmpty() {
        final var actualType = CastMemberType.of(null);

        assertTrue(actualType.isEmpty());
    }
}
