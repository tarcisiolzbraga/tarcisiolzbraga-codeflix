package com.tarcisiolzbraga.codeflix.admin.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VideoResourceTest {

    private static final Resource RESOURCE = Resource.with(new byte[] {1}, "abc123", "video/mp4", "duna.mp4");

    @Test
    void givenValidParams_whenCallWith_thenInstantiateIt() {
        final var actualResource = VideoResource.with(VideoMediaType.TRAILER, RESOURCE);

        assertEquals(VideoMediaType.TRAILER, actualResource.type());
        assertEquals(RESOURCE, actualResource.resource());
    }

    @Test
    void givenNullType_whenCallWith_thenReceiveAnError() {
        final var actualException =
                assertThrows(NullPointerException.class, () -> VideoResource.with(null, RESOURCE));

        assertEquals("'type' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullResource_whenCallWith_thenReceiveAnError() {
        final var actualException =
                assertThrows(NullPointerException.class, () -> VideoResource.with(VideoMediaType.VIDEO, null));

        assertEquals("'resource' should not be null", actualException.getMessage());
    }
}
