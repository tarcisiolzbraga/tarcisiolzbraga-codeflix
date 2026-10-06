package com.tarcisiolzbraga.codeflix.videos.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VideoIDTest {

    private static final String EXPECTED_VALUE = "9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608";

    @Test
    void givenAValue_whenCallFrom_thenHoldIt() {
        final var actualId = VideoID.from(EXPECTED_VALUE);

        assertEquals(EXPECTED_VALUE, actualId.getValue());
    }

    @Test
    void givenNullValue_whenCallFrom_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> VideoID.from(null));

        assertEquals("'value' should not be null", actualException.getMessage());
    }

    @Test
    void givenTwoIdsWithTheSameValue_whenCompare_thenBeEqual() {
        assertEquals(VideoID.from(EXPECTED_VALUE), VideoID.from(EXPECTED_VALUE));
    }
}
