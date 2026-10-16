package com.tarcisiolzbraga.codeflix.admin.application.video.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoFields;
import com.tarcisiolzbraga.codeflix.admin.application.video.VideoReferenceIds;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoFixture;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoReferences;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateVideoUseCaseTest {

    private static final String OLD_TITLE = "Duna";
    private static final String EXPECTED_TITLE = "Duna: Parte 2";
    private static final String CATEGORY_ID = "11111111-1111-1111-1111-111111111111";

    @Mock
    private CategoryGateway categoryGateway;

    @Mock
    private GenreGateway genreGateway;

    @Mock
    private CastMemberGateway castMemberGateway;

    @Mock
    private VideoGateway videoGateway;

    @InjectMocks
    private DefaultUpdateVideoUseCase useCase;

    @Test
    void givenValidCommand_whenCallExecute_thenReplaceFieldsAndReferences() {
        final var video = givenStoredVideo();
        final var command = UpdateVideoCommand.with(
                video.getId().getValue(),
                new VideoFields(EXPECTED_TITLE, "Arrakis", 2024, 166.0, "14"),
                new VideoReferenceIds(Set.of(CATEGORY_ID), Set.of(), Set.of()));
        when(categoryGateway.findExistingIds(any())).thenReturn(Set.of(CategoryID.from(CATEGORY_ID)));
        when(videoGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        verify(videoGateway).update(argThat(updated -> EXPECTED_TITLE.equals(updated.getTitle())
                && Rating.AGE_14 == updated.getRating()
                && updated.getCategories().equals(Set.of(CategoryID.from(CATEGORY_ID)))));
    }

    @Test
    void givenUnknownCategory_whenCallExecute_thenReturnLeftWithoutUpdating() {
        final var video = givenStoredVideo();
        final var command = UpdateVideoCommand.with(
                video.getId().getValue(),
                new VideoFields(EXPECTED_TITLE, "Arrakis", 2024, 166.0, "14"),
                new VideoReferenceIds(Set.of(CATEGORY_ID), Set.of(), Set.of()));
        when(categoryGateway.findExistingIds(any())).thenReturn(Set.of());

        final var actualResult = useCase.execute(command);

        assertEquals(
                List.of("Some categories could not be found: " + CATEGORY_ID),
                messagesOf(actualResult.getLeft().getErrors()));
        verify(videoGateway, never()).update(any());
    }

    @Test
    void givenNullTitleAndUnknownRating_whenCallExecute_thenReturnLeftWithBothErrors() {
        final var video = givenStoredVideo();
        final var command = UpdateVideoCommand.with(
                video.getId().getValue(),
                new VideoFields(null, "Arrakis", 2024, 166.0, "99"),
                VideoReferenceIds.none());

        final var actualResult = useCase.execute(command);

        assertEquals(
                List.of("'title' should not be null", "'rating' should not be null"),
                messagesOf(actualResult.getLeft().getErrors()));
        verify(videoGateway, never()).update(any());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = VideoID.unique();
        final var command = UpdateVideoCommand.with(
                expectedId.getValue(),
                new VideoFields(EXPECTED_TITLE, "Arrakis", 2024, 166.0, "14"),
                VideoReferenceIds.none());
        when(videoGateway.findById(any())).thenReturn(Optional.empty());

        final var actualException = assertThrows(NotFoundException.class, () -> useCase.execute(command));

        assertEquals("Video with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }

    private Video givenStoredVideo() {
        final var video = Video.newVideo(
                VideoFixture.details(OLD_TITLE), VideoReferences.none());
        when(videoGateway.findById(any())).thenReturn(Optional.of(video));
        return video;
    }

    private List<String> messagesOf(final List<ValidationError> errors) {
        return errors.stream().map(ValidationError::message).toList();
    }
}
