package com.tarcisiolzbraga.codeflix.admin.domain.video;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResourceTest {

    private static final String CHECKSUM = "e5c7a1f0";
    private static final String CONTENT_TYPE = "video/mp4";
    private static final String NAME = "duna.mp4";

    @Test
    void givenValidParams_whenCallWith_thenInstantiateIt() {
        final var expectedContent = new byte[] {1, 2, 3};

        final var actualResource = Resource.with(expectedContent, CHECKSUM, CONTENT_TYPE, NAME);

        assertArrayEquals(expectedContent, actualResource.content());
        assertEquals(CHECKSUM, actualResource.checksum());
        assertEquals(CONTENT_TYPE, actualResource.contentType());
        assertEquals(NAME, actualResource.name());
    }

    @Test
    void givenAResource_whenCallerChangesTheArrayItPassed_thenKeepTheOriginalContent() {
        final var givenContent = new byte[] {1, 2, 3};
        final var actualResource = Resource.with(givenContent, CHECKSUM, CONTENT_TYPE, NAME);

        givenContent[0] = 9;

        assertArrayEquals(new byte[] {1, 2, 3}, actualResource.content());
    }

    @Test
    void givenAResource_whenCallerChangesTheArrayItReceived_thenKeepTheOriginalContent() {
        final var actualResource = Resource.with(new byte[] {1, 2, 3}, CHECKSUM, CONTENT_TYPE, NAME);

        actualResource.content()[0] = 9;

        assertArrayEquals(new byte[] {1, 2, 3}, actualResource.content());
    }

    @Test
    void givenTwoResourcesWithTheSameValues_whenCallEquals_thenBeEqual() {
        final var one = Resource.with(new byte[] {1, 2, 3}, CHECKSUM, CONTENT_TYPE, NAME);
        final var other = Resource.with(new byte[] {1, 2, 3}, CHECKSUM, CONTENT_TYPE, NAME);

        final var actualEquality = one.equals(other);

        assertTrue(actualEquality);
        assertEquals(other.hashCode(), one.hashCode());
    }

    @Test
    void givenTwoResourcesWithDifferentContent_whenCallEquals_thenNotBeEqual() {
        final var one = Resource.with(new byte[] {1, 2, 3}, CHECKSUM, CONTENT_TYPE, NAME);
        final var other = Resource.with(new byte[] {4, 5, 6}, CHECKSUM, CONTENT_TYPE, NAME);

        final var actualEquality = one.equals(other);

        assertFalse(actualEquality);
    }

    @Test
    void givenNullContent_whenCallWith_thenReceiveAnError() {
        final var actualException = assertThrows(
                NullPointerException.class, () -> Resource.with(null, CHECKSUM, CONTENT_TYPE, NAME));

        assertEquals("'content' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullChecksum_whenCallWith_thenReceiveAnError() {
        final var actualException = assertThrows(
                NullPointerException.class, () -> Resource.with(new byte[] {1}, null, CONTENT_TYPE, NAME));

        assertEquals("'checksum' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullContentType_whenCallWith_thenReceiveAnError() {
        final var actualException = assertThrows(
                NullPointerException.class, () -> Resource.with(new byte[] {1}, CHECKSUM, null, NAME));

        assertEquals("'contentType' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullName_whenCallWith_thenReceiveAnError() {
        final var actualException = assertThrows(
                NullPointerException.class, () -> Resource.with(new byte[] {1}, CHECKSUM, CONTENT_TYPE, null));

        assertEquals("'name' should not be null", actualException.getMessage());
    }
}
