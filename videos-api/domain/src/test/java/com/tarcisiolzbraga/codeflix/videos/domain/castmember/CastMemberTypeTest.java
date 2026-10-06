package com.tarcisiolzbraga.codeflix.videos.domain.castmember;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CastMemberTypeTest {

    @Test
    void givenAKnownName_whenCallOf_thenReturnTheType() {
        final var actualType = CastMemberType.of("DIRECTOR");

        assertEquals(CastMemberType.DIRECTOR, actualType.orElseThrow());
    }

    @Test
    void givenAKnownNameInAnotherCase_whenCallOf_thenStillReturnTheType() {
        final var actualType = CastMemberType.of("actor");

        assertEquals(CastMemberType.ACTOR, actualType.orElseThrow());
    }

    @Test
    void givenAnUnknownName_whenCallOf_thenReturnEmptyInsteadOfThrowing() {
        final var actualType = CastMemberType.of("PRODUTOR");

        assertTrue(actualType.isEmpty());
    }

    @Test
    void givenNull_whenCallOf_thenReturnEmpty() {
        final var actualType = CastMemberType.of(null);

        assertTrue(actualType.isEmpty());
    }
}
