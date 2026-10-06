package com.tarcisiolzbraga.codeflix.videos.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RatingTest {

    @ParameterizedTest
    @CsvSource({"ER,ER", "L,L", "10,AGE_10", "12,AGE_12", "14,AGE_14", "16,AGE_16", "18,AGE_18"})
    void givenAKnownLabel_whenCallOf_thenReturnTheRating(final String label, final String expected) {
        final var actualRating = Rating.of(label);

        assertEquals(Rating.valueOf(expected), actualRating.orElseThrow());
    }

    @Test
    void givenAKnownLabelInAnotherCase_whenCallOf_thenStillReturnIt() {
        assertEquals(Rating.ER, Rating.of("er").orElseThrow());
    }

    @Test
    void givenAnUnknownLabel_whenCallOf_thenReturnEmptyInsteadOfThrowing() {
        assertTrue(Rating.of("21").isEmpty());
    }

    @Test
    void givenNull_whenCallOf_thenReturnEmpty() {
        assertTrue(Rating.of(null).isEmpty());
    }

    // O rótulo é o que vem na mensagem, e não coincide com o nome da constante nas faixas de idade.
    @Test
    void givenTheAgeRatings_whenCallGetLabel_thenReturnOnlyTheNumber() {
        assertEquals("10", Rating.AGE_10.getLabel());
        assertEquals("18", Rating.AGE_18.getLabel());
    }
}
