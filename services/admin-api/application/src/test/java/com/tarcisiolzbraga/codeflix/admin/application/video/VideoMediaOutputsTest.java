package com.tarcisiolzbraga.codeflix.admin.application.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tarcisiolzbraga.codeflix.admin.domain.video.AudioVideoMedia;
import com.tarcisiolzbraga.codeflix.admin.domain.video.ImageMedia;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoFixture;
import org.junit.jupiter.api.Test;

class VideoMediaOutputsTest {

    @Test
    void givenVideoWithoutMedias_whenCallFrom_thenLeaveEveryMediaNull() {
        final var actualOutputs = VideoMediaOutputs.from(VideoFixture.video());

        assertNull(actualOutputs.video());
        assertNull(actualOutputs.trailer());
        assertNull(actualOutputs.banner());
        assertNull(actualOutputs.thumbnail());
        assertNull(actualOutputs.thumbnailHalf());
    }

    @Test
    void givenVideoWithAnAudioVideoMedia_whenCallFrom_thenCarryItsStatusAsText() {
        final var video = VideoFixture.video();
        video.updateVideoMedia(AudioVideoMedia.with("abc1", "duna.mp4", "raw/video"));

        final var actualOutputs = VideoMediaOutputs.from(video);

        assertEquals("abc1", actualOutputs.video().checksum());
        assertEquals("duna.mp4", actualOutputs.video().name());
        assertEquals("raw/video", actualOutputs.video().rawLocation());
        assertEquals("", actualOutputs.video().encodedLocation());
        assertEquals("PENDING", actualOutputs.video().status());
    }

    @Test
    void givenVideoWithAnImageMedia_whenCallFrom_thenCarryItsSingleLocation() {
        final var video = VideoFixture.video();
        video.updateBanner(ImageMedia.with("abc2", "duna.png", "raw/banner"));

        final var actualOutputs = VideoMediaOutputs.from(video);

        assertEquals("abc2", actualOutputs.banner().checksum());
        assertEquals("duna.png", actualOutputs.banner().name());
        assertEquals("raw/banner", actualOutputs.banner().location());
    }
}
