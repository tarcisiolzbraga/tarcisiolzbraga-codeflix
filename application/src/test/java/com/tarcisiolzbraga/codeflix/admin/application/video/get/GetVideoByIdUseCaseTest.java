package com.tarcisiolzbraga.codeflix.admin.application.video.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoDetails;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoReferences;
import java.time.Year;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetVideoByIdUseCaseTest {

    private static final String EXPECTED_TITLE = "Duna";

    @Mock
    private VideoGateway videoGateway;

    @InjectMocks
    private DefaultGetVideoByIdUseCase useCase;

    @Test
    void givenExistingId_whenCallExecute_thenReturnTheVideoInEdgeTypes() {
        final var category = CategoryID.unique();
        final var video = Video.newVideo(
                VideoDetails.with(EXPECTED_TITLE, "Arrakis", Year.of(2021), 155.0, Rating.AGE_12),
                VideoReferences.with(Set.of(category), Set.of(), Set.of()));
        when(videoGateway.findById(any())).thenReturn(Optional.of(video));

        final var actualOutput = useCase.execute(video.getId().getValue());

        assertEquals(video.getId().getValue(), actualOutput.id());
        assertEquals(EXPECTED_TITLE, actualOutput.fields().title());
        assertEquals(2021, actualOutput.fields().launchedAt());
        assertEquals("12", actualOutput.fields().rating());
        assertEquals(Set.of(category.getValue()), actualOutput.references().categories());
        assertTrue(actualOutput.active());
        assertFalse(actualOutput.published());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = VideoID.unique();
        when(videoGateway.findById(any())).thenReturn(Optional.empty());

        final var actualException =
                assertThrows(NotFoundException.class, () -> useCase.execute(expectedId.getValue()));

        assertEquals("Video with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }
}
