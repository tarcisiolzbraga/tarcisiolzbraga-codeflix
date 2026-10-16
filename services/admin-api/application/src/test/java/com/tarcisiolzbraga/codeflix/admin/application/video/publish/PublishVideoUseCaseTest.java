package com.tarcisiolzbraga.codeflix.admin.application.video.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoDetails;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoReferences;
import java.time.Year;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublishVideoUseCaseTest {

    private static final String EXPECTED_TITLE = "Duna";

    @Mock
    private VideoGateway videoGateway;

    @InjectMocks
    private DefaultPublishVideoUseCase useCase;

    @Test
    void givenVideoInTheOtherState_whenCallExecute_thenPublishIt() {
        final var video = givenStoredVideo();
        when(videoGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualOutput = useCase.execute(video.getId().getValue());

        assertEquals(video.getId().getValue(), actualOutput.id());
        assertTrue(actualOutput.published());
    }

    @Test
    void givenVideoAlreadyInTheState_whenCallExecute_thenKeepIt() {
        final var video = givenStoredVideo();
        video.publish();
        when(videoGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualOutput = useCase.execute(video.getId().getValue());

        assertTrue(actualOutput.published());
        verify(videoGateway).update(video);
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFoundAndNotUpdate() {
        final var expectedId = VideoID.unique();
        when(videoGateway.findById(expectedId)).thenReturn(Optional.empty());

        final var actualException =
                assertThrows(NotFoundException.class, () -> useCase.execute(expectedId.getValue()));

        assertEquals("Video with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
        verify(videoGateway, never()).update(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var video = givenStoredVideo();
        final var expectedException = new IllegalStateException("gateway indisponível");
        when(videoGateway.update(any())).thenThrow(expectedException);

        final var actualException =
                assertThrows(IllegalStateException.class, () -> useCase.execute(video.getId().getValue()));

        assertSame(expectedException, actualException);
    }

    private Video givenStoredVideo() {
        final var video = Video.newVideo(
                VideoDetails.with(EXPECTED_TITLE, "Arrakis", Year.of(2021), 155.0, Rating.AGE_12),
                VideoReferences.none());
        when(videoGateway.findById(video.getId())).thenReturn(Optional.of(video));
        return video;
    }
}
