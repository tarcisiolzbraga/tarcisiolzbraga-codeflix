package com.tarcisiolzbraga.codeflix.videos.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class VideoMediasTest {

    @Test
    void givenTheFiveAddresses_whenCallWith_thenHoldThem() {
        final var actualMedias = VideoMedias.with("v.mp4", "t.mp4", "b.jpg", "th.jpg", "thh.jpg");

        assertEquals("v.mp4", actualMedias.video());
        assertEquals("t.mp4", actualMedias.trailer());
        assertEquals("b.jpg", actualMedias.banner());
        assertEquals("th.jpg", actualMedias.thumbnail());
        assertEquals("thh.jpg", actualMedias.thumbnailHalf());
    }

    // O vídeo existe no admin antes de qualquer arquivo ser enviado, então réplica sem mídia é
    // estado normal, não erro.
    @Test
    void givenNothing_whenCallNone_thenHoldNoAddress() {
        final var actualMedias = VideoMedias.none();

        assertNull(actualMedias.video());
        assertNull(actualMedias.trailer());
        assertNull(actualMedias.banner());
        assertNull(actualMedias.thumbnail());
        assertNull(actualMedias.thumbnailHalf());
    }
}
