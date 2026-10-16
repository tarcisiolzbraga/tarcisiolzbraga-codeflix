package com.tarcisiolzbraga.codeflix.admin.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RatingTest {

    @Test
    void givenALabel_whenCallOf_thenReturnTheRating() {
        final var actualRating = Rating.of("12");

        assertEquals(Optional.of(Rating.AGE_12), actualRating);
    }

    @Test
    void givenALabelInAnotherCase_whenCallOf_thenReturnTheRating() {
        final var actualRating = Rating.of("er");

        assertEquals(Optional.of(Rating.ER), actualRating);
    }

    @Test
    void givenUnknownLabel_whenCallOf_thenReturnEmpty() {
        final var actualRating = Rating.of("99");

        assertTrue(actualRating.isEmpty());
    }

    @Test
    void givenNullValue_whenCallOf_thenReturnEmpty() {
        final var actualRating = Rating.of(null);

        assertTrue(actualRating.isEmpty());
    }

    @Test
    void givenEveryRating_whenCallGetLabel_thenReturnWhatTheApiExposes() {
        final var actualLabels = Arrays.stream(Rating.values()).map(Rating::getLabel).toList();

        assertEquals(List.of("ER", "L", "10", "12", "14", "16", "18"), actualLabels);
    }
}
