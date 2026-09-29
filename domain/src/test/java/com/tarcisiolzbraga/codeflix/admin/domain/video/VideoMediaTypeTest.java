package com.tarcisiolzbraga.codeflix.admin.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class VideoMediaTypeTest {

    @Test
    void givenAKnownName_whenCallOf_thenReceiveTheType() {
        final var actualType = VideoMediaType.of("TRAILER");

        assertEquals(Optional.of(VideoMediaType.TRAILER), actualType);
    }

    @Test
    void givenAKnownNameInAnotherCase_whenCallOf_thenReceiveTheType() {
        final var actualType = VideoMediaType.of("thumbnail_half");

        assertEquals(Optional.of(VideoMediaType.THUMBNAIL_HALF), actualType);
    }

    @Test
    void givenAnUnknownName_whenCallOf_thenReceiveEmpty() {
        final var actualType = VideoMediaType.of("poster");

        assertTrue(actualType.isEmpty());
    }

    @Test
    void givenANullName_whenCallOf_thenReceiveEmpty() {
        final var actualType = VideoMediaType.of(null);

        assertTrue(actualType.isEmpty());
    }
}
