package com.tarcisiolzbraga.codeflix.admin.application.video.media.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.video.AudioVideoMedia;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaStatus;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoFixture;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class UpdateMediaStatusUseCaseIT {

    private static final String ENCODED_PATH = "encoded/video.mp4";
    private static final String CHECKSUM = "abc1";

    @Autowired
    private UpdateMediaStatusUseCase useCase;

    @Autowired
    private VideoGateway videoGateway;

    @Test
    void givenCompleted_whenCallExecute_thenTheStoredVideoKeepsTheEncodedPath() {
        final var video = givenStoredVideoWithMedia();

        useCase.execute(commandWith(video.getId(), MediaStatus.COMPLETED));

        final var actualMedia =
                this.videoGateway.findById(video.getId()).orElseThrow().getVideo().orElseThrow();
        assertEquals(MediaStatus.COMPLETED, actualMedia.status());
        assertEquals(ENCODED_PATH, actualMedia.encodedLocation());
    }

    @Test
    void givenProcessing_whenCallExecute_thenTheStoredVideoMovesWithoutAnEncodedPath() {
        final var video = givenStoredVideoWithMedia();

        useCase.execute(commandWith(video.getId(), MediaStatus.PROCESSING));

        final var actualMedia =
                this.videoGateway.findById(video.getId()).orElseThrow().getVideo().orElseThrow();
        assertEquals(MediaStatus.PROCESSING, actualMedia.status());
        assertEquals("", actualMedia.encodedLocation());
    }

    @Test
    void givenAMediaTheVideoDoesNotHave_whenCallExecute_thenLeaveItAsItWas() {
        final var video = givenStoredVideoWithMedia();

        useCase.execute(new UpdateMediaStatusCommand(
                video.getId().getValue(), VideoMediaType.TRAILER, MediaStatus.COMPLETED, CHECKSUM, ENCODED_PATH));

        final var reloaded = this.videoGateway.findById(video.getId()).orElseThrow();
        assertEquals(MediaStatus.PENDING, reloaded.getVideo().orElseThrow().status());
    }

    @Test
    void givenUnknownVideo_whenCallExecute_thenThrowNotFound() {
        final var expectedId = VideoID.unique();
        final var command = commandWith(expectedId, MediaStatus.COMPLETED);

        final var actualException = assertThrows(NotFoundException.class, () -> useCase.execute(command));

        assertEquals("Video with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }

    private Video givenStoredVideoWithMedia() {
        final var video = VideoFixture.video();
        video.updateVideoMedia(AudioVideoMedia.with("abc1", "duna.mp4", "raw/video"));
        return this.videoGateway.create(video);
    }

    private UpdateMediaStatusCommand commandWith(final VideoID id, final MediaStatus status) {
        return new UpdateMediaStatusCommand(
                id.getValue(), VideoMediaType.VIDEO, status, CHECKSUM, ENCODED_PATH);
    }
}
