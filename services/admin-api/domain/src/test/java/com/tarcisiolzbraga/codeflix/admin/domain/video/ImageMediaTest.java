package com.tarcisiolzbraga.codeflix.admin.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ImageMediaTest {

    private static final String CHECKSUM = "e5c7a1f0";
    private static final String NAME = "duna.png";
    private static final String LOCATION = "videoId-BANNER";

    @Test
    void givenValidParams_whenCallWith_thenInstantiateIt() {
        final var actualMedia = ImageMedia.with(CHECKSUM, NAME, LOCATION);

        assertEquals(CHECKSUM, actualMedia.checksum());
        assertEquals(NAME, actualMedia.name());
        assertEquals(LOCATION, actualMedia.location());
    }

    @Test
    void givenTwoMediasWithTheSameValues_whenCallEquals_thenBeEqual() {
        final var one = ImageMedia.with(CHECKSUM, NAME, LOCATION);
        final var other = ImageMedia.with(CHECKSUM, NAME, LOCATION);

        final var actualEquality = one.equals(other);

        assertEquals(other.hashCode(), one.hashCode());
        assertTrue(actualEquality);
    }

    @Test
    void givenNullChecksum_whenCallWith_thenReceiveAnError() {
        final var actualException =
                assertThrows(NullPointerException.class, () -> ImageMedia.with(null, NAME, LOCATION));

        assertEquals("'checksum' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullName_whenCallWith_thenReceiveAnError() {
        final var actualException =
                assertThrows(NullPointerException.class, () -> ImageMedia.with(CHECKSUM, null, LOCATION));

        assertEquals("'name' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullLocation_whenCallWith_thenReceiveAnError() {
        final var actualException =
                assertThrows(NullPointerException.class, () -> ImageMedia.with(CHECKSUM, NAME, null));

        assertEquals("'location' should not be null", actualException.getMessage());
    }
}
