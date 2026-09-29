package com.tarcisiolzbraga.codeflix.admin.application.video.media.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaResourceGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaStatus;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Resource;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoFixture;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoResource;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class UploadMediaUseCaseIT {

    @Autowired
    private UploadMediaUseCase useCase;

    @Autowired
    private VideoGateway videoGateway;

    @MockitoSpyBean
    private MediaResourceGateway mediaResourceGateway;

    @Test
    void givenAVideoFile_whenCallExecute_thenStoreItAndLeaveTheVideoPointingToIt() {
        final var video = givenStoredVideo();

        final var actualOutput = useCase.execute(commandFor(video.getId(), VideoMediaType.VIDEO));

        assertEquals(video.getId().getValue(), actualOutput.videoId());
        final var reloaded = this.videoGateway.findById(video.getId()).orElseThrow();
        assertEquals("%s/VIDEO".formatted(video.getId().getValue()), reloaded.getVideo().orElseThrow().rawLocation());
        assertEquals(MediaStatus.PENDING, reloaded.getVideo().orElseThrow().status());
    }

    @Test
    void givenABannerFile_whenCallExecute_thenLeaveTheVideoPointingToTheImage() {
        final var video = givenStoredVideo();

        useCase.execute(commandFor(video.getId(), VideoMediaType.BANNER));

        final var reloaded = this.videoGateway.findById(video.getId()).orElseThrow();
        assertEquals("%s/BANNER".formatted(video.getId().getValue()), reloaded.getBanner().orElseThrow().location());
        assertTrue(reloaded.getVideo().isEmpty());
    }

    @Test
    void givenUnknownVideo_whenCallExecute_thenThrowNotFound() {
        final var expectedId = VideoID.unique();
        final var command = commandFor(expectedId, VideoMediaType.VIDEO);

        final var actualException = assertThrows(NotFoundException.class, () -> useCase.execute(command));

        assertEquals("Video with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }

    @Test
    void givenFailingStorage_whenCallExecute_thenPropagateTheException() {
        final var video = givenStoredVideo();
        final var expectedException = new IllegalStateException("armazenamento indisponível");
        doThrow(expectedException).when(this.mediaResourceGateway).storeAudioVideo(any(), any());
        final var command = commandFor(video.getId(), VideoMediaType.VIDEO);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        assertSame(expectedException, actualException);
    }

    private Video givenStoredVideo() {
        return this.videoGateway.create(VideoFixture.video());
    }

    private UploadMediaCommand commandFor(final VideoID id, final VideoMediaType type) {
        final var resource = Resource.with("conteudo".getBytes(), "abc1", "video/mp4", "duna.mp4");
        return new UploadMediaCommand(id.getValue(), VideoResource.with(type, resource));
    }
}
