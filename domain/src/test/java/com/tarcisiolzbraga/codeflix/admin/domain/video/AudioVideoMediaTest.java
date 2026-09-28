package com.tarcisiolzbraga.codeflix.admin.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AudioVideoMediaTest {

    private static final String CHECKSUM = "e5c7a1f0";
    private static final String NAME = "duna.mp4";
    private static final String RAW_LOCATION = "videoId-VIDEO";
    private static final String ENCODED_LOCATION = "videoId-VIDEO-encoded";

    @Test
    void givenValidParams_whenCallWith_thenBeBornPendingAndNotEncoded() {
        final var actualMedia = AudioVideoMedia.with(CHECKSUM, NAME, RAW_LOCATION);

        assertEquals(CHECKSUM, actualMedia.checksum());
        assertEquals(NAME, actualMedia.name());
        assertEquals(RAW_LOCATION, actualMedia.rawLocation());
        assertEquals("", actualMedia.encodedLocation());
        assertEquals(MediaStatus.PENDING, actualMedia.status());
    }

    @Test
    void givenAStoredMedia_whenCallWith_thenRebuildItAsItWas() {
        final var actualMedia =
                AudioVideoMedia.with(CHECKSUM, NAME, RAW_LOCATION, ENCODED_LOCATION, MediaStatus.COMPLETED);

        assertEquals(ENCODED_LOCATION, actualMedia.encodedLocation());
        assertEquals(MediaStatus.COMPLETED, actualMedia.status());
    }

    @Test
    void givenTwoMediasWithTheSameValues_whenCallEquals_thenBeEqual() {
        final var one = AudioVideoMedia.with(CHECKSUM, NAME, RAW_LOCATION);
        final var other = AudioVideoMedia.with(CHECKSUM, NAME, RAW_LOCATION);

        final var actualEquality = one.equals(other);

        assertEquals(other.hashCode(), one.hashCode());
        assertTrue(actualEquality);
    }

    @Test
    void givenNullChecksum_whenCallWith_thenReceiveAnError() {
        final var actualException =
                assertThrows(NullPointerException.class, () -> AudioVideoMedia.with(null, NAME, RAW_LOCATION));

        assertEquals("'checksum' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullName_whenCallWith_thenReceiveAnError() {
        final var actualException =
                assertThrows(NullPointerException.class, () -> AudioVideoMedia.with(CHECKSUM, null, RAW_LOCATION));

        assertEquals("'name' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullRawLocation_whenCallWith_thenReceiveAnError() {
        final var actualException =
                assertThrows(NullPointerException.class, () -> AudioVideoMedia.with(CHECKSUM, NAME, null));

        assertEquals("'rawLocation' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullStatus_whenCallWith_thenReceiveAnError() {
        final var actualException = assertThrows(
                NullPointerException.class,
                () -> AudioVideoMedia.with(CHECKSUM, NAME, RAW_LOCATION, ENCODED_LOCATION, null));

        assertEquals("'status' should not be null", actualException.getMessage());
    }
}
