package com.tarcisiolzbraga.codeflix.admin.application.video.media.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.video.AudioVideoMedia;
import com.tarcisiolzbraga.codeflix.admin.domain.video.ImageMedia;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaResourceGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Resource;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoFixture;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoResource;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UploadMediaUseCaseTest {

    private static final AudioVideoMedia AUDIO_VIDEO = AudioVideoMedia.with("abc1", "duna.mp4", "raw/video");
    private static final ImageMedia IMAGE = ImageMedia.with("abc2", "duna.png", "raw/image");

    @Mock
    private VideoGateway videoGateway;

    @Mock
    private MediaResourceGateway mediaResourceGateway;

    @InjectMocks
    private DefaultUploadMediaUseCase useCase;

    @Test
    void givenAVideoFile_whenCallExecute_thenStoreItAndAttachItToTheVideo() {
        final var video = givenStoredVideo();
        when(mediaResourceGateway.storeAudioVideo(any(), any())).thenReturn(AUDIO_VIDEO);
        when(videoGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualOutput = useCase.execute(commandFor(video.getId(), VideoMediaType.VIDEO));

        assertEquals(video.getId().getValue(), actualOutput.videoId());
        assertEquals(VideoMediaType.VIDEO, actualOutput.type());
        assertEquals(Optional.of(AUDIO_VIDEO), video.getVideo());
    }

    @Test
    void givenATrailerFile_whenCallExecute_thenAttachItAsTheTrailer() {
        final var video = givenStoredVideo();
        when(mediaResourceGateway.storeAudioVideo(any(), any())).thenReturn(AUDIO_VIDEO);
        when(videoGateway.update(any())).thenAnswer(returnsFirstArg());

        useCase.execute(commandFor(video.getId(), VideoMediaType.TRAILER));

        assertEquals(Optional.of(AUDIO_VIDEO), video.getTrailer());
    }

    @Test
    void givenABannerFile_whenCallExecute_thenAttachItAsAnImage() {
        final var video = givenStoredVideo();
        when(mediaResourceGateway.storeImage(any(), any())).thenReturn(IMAGE);
        when(videoGateway.update(any())).thenAnswer(returnsFirstArg());

        useCase.execute(commandFor(video.getId(), VideoMediaType.BANNER));

        assertEquals(Optional.of(IMAGE), video.getBanner());
    }

    @Test
    void givenAThumbnailHalfFile_whenCallExecute_thenAttachItAsAnImage() {
        final var video = givenStoredVideo();
        when(mediaResourceGateway.storeImage(any(), any())).thenReturn(IMAGE);
        when(videoGateway.update(any())).thenAnswer(returnsFirstArg());

        useCase.execute(commandFor(video.getId(), VideoMediaType.THUMBNAIL_HALF));

        assertEquals(Optional.of(IMAGE), video.getThumbnailHalf());
    }

    @Test
    void givenUnknownVideo_whenCallExecute_thenThrowNotFoundAndStoreNothing() {
        final var expectedId = VideoID.unique();
        when(videoGateway.findById(expectedId)).thenReturn(Optional.empty());
        final var command = commandFor(expectedId, VideoMediaType.VIDEO);

        final var actualException = assertThrows(NotFoundException.class, () -> useCase.execute(command));

        assertEquals("Video with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
        verify(mediaResourceGateway, never()).storeAudioVideo(any(), any());
    }

    @Test
    void givenFailingStorage_whenCallExecute_thenPropagateTheExceptionAndNotUpdate() {
        final var video = givenStoredVideo();
        final var expectedException = new IllegalStateException("armazenamento indisponível");
        when(mediaResourceGateway.storeAudioVideo(any(), any())).thenThrow(expectedException);
        final var command = commandFor(video.getId(), VideoMediaType.VIDEO);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        assertSame(expectedException, actualException);
        verify(videoGateway, never()).update(any());
    }

    private Video givenStoredVideo() {
        final var video = VideoFixture.video();
        when(videoGateway.findById(video.getId())).thenReturn(Optional.of(video));
        return video;
    }

    private UploadMediaCommand commandFor(final VideoID id, final VideoMediaType type) {
        final var resource = Resource.with("conteudo".getBytes(), "abc1", "video/mp4", "duna.mp4");
        return new UploadMediaCommand(id.getValue(), VideoResource.with(type, resource));
    }
}
