package com.tarcisiolzbraga.codeflix.admin.application.video.media.update;

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
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaStatus;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoFixture;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateMediaStatusUseCaseTest {

    private static final String ENCODED_PATH = "encoded/video.mp4";

    @Mock
    private VideoGateway videoGateway;

    @InjectMocks
    private DefaultUpdateMediaStatusUseCase useCase;

    @Test
    void givenProcessing_whenCallExecute_thenMoveTheMediaAndSaveTheVideo() {
        final var video = givenVideoWithMedia();
        when(videoGateway.update(any())).thenAnswer(returnsFirstArg());

        useCase.execute(commandWith(video.getId(), MediaStatus.PROCESSING));

        assertEquals(MediaStatus.PROCESSING, video.getVideo().orElseThrow().status());
        verify(videoGateway).update(video);
    }

    @Test
    void givenCompleted_whenCallExecute_thenKeepWhereTheEncodedFileIs() {
        final var video = givenVideoWithMedia();
        when(videoGateway.update(any())).thenAnswer(returnsFirstArg());

        useCase.execute(commandWith(video.getId(), MediaStatus.COMPLETED));

        final var actualMedia = video.getVideo().orElseThrow();
        assertEquals(MediaStatus.COMPLETED, actualMedia.status());
        assertEquals(ENCODED_PATH, actualMedia.encodedLocation());
    }

    @Test
    void givenPending_whenCallExecute_thenNotEvenLookForTheVideo() {
        final var command = commandWith(VideoID.unique(), MediaStatus.PENDING);

        useCase.execute(command);

        verify(videoGateway, never()).findById(any());
        verify(videoGateway, never()).update(any());
    }

    @Test
    void givenUnknownVideo_whenCallExecute_thenThrowNotFoundAndSaveNothing() {
        final var expectedId = VideoID.unique();
        when(videoGateway.findById(expectedId)).thenReturn(Optional.empty());
        final var command = commandWith(expectedId, MediaStatus.COMPLETED);

        final var actualException = assertThrows(NotFoundException.class, () -> useCase.execute(command));

        assertEquals("Video with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
        verify(videoGateway, never()).update(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var video = givenVideoWithMedia();
        final var expectedException = new IllegalStateException("gateway indisponível");
        when(videoGateway.update(any())).thenThrow(expectedException);
        final var command = commandWith(video.getId(), MediaStatus.COMPLETED);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        assertSame(expectedException, actualException);
    }

    private Video givenVideoWithMedia() {
        final var video = VideoFixture.video();
        video.updateVideoMedia(AudioVideoMedia.with("abc1", "duna.mp4", "raw/video"));
        when(videoGateway.findById(video.getId())).thenReturn(Optional.of(video));
        return video;
    }

    private UpdateMediaStatusCommand commandWith(final VideoID id, final MediaStatus status) {
        return new UpdateMediaStatusCommand(id.getValue(), VideoMediaType.VIDEO, status, ENCODED_PATH);
    }
}
