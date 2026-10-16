package com.tarcisiolzbraga.codeflix.admin.application.video.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateVideoUseCaseTest {

    private static final String EXPECTED_TITLE = "Duna";
    private static final String EXPECTED_DESCRIPTION = "Paul Atreides em Arrakis";
    private static final String CATEGORY_ID = "11111111-1111-1111-1111-111111111111";
    private static final String GENRE_ID = "22222222-2222-2222-2222-222222222222";
    private static final String MEMBER_ID = "33333333-3333-3333-3333-333333333333";
    private static final String RATING_MESSAGE = "'rating' should not be null";

    @Mock
    private CategoryGateway categoryGateway;

    @Mock
    private GenreGateway genreGateway;

    @Mock
    private CastMemberGateway castMemberGateway;

    @Mock
    private VideoGateway videoGateway;

    @InjectMocks
    private DefaultCreateVideoUseCase useCase;

    @Test
    void givenValidCommand_whenCallExecute_thenReturnRightWithVideoId() {
        final var command = commandWithReferences();
        when(categoryGateway.findExistingIds(any())).thenReturn(Set.of(CategoryID.from(CATEGORY_ID)));
        when(genreGateway.findExistingIds(any())).thenReturn(Set.of(GenreID.from(GENRE_ID)));
        when(castMemberGateway.findExistingIds(any())).thenReturn(Set.of(CastMemberID.from(MEMBER_ID)));
        when(videoGateway.create(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        assertNotNull(actualResult.get().id());
        verify(videoGateway).create(argThat(CreateVideoUseCaseTest::isCreatedFromCommand));
    }

    @Test
    void givenCommandWithoutReferences_whenCallExecute_thenNotQueryTheOtherGateways() {
        final var command = CreateVideoCommand.with(
                new VideoFields(EXPECTED_TITLE, EXPECTED_DESCRIPTION, 2021, 155.0, "12"), VideoReferenceIds.none());
        when(videoGateway.create(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        verify(categoryGateway, never()).findExistingIds(any());
        verify(genreGateway, never()).findExistingIds(any());
        verify(castMemberGateway, never()).findExistingIds(any());
    }

    @Test
    void givenUnknownRating_whenCallExecute_thenReturnLeftWithASingleError() {
        final var command = CreateVideoCommand.with(
                new VideoFields(EXPECTED_TITLE, EXPECTED_DESCRIPTION, 2021, 155.0, "99"), VideoReferenceIds.none());

        final var actualResult = useCase.execute(command);

        assertEquals(List.of(RATING_MESSAGE), messagesOf(actualResult.getLeft().getErrors()));
        verify(videoGateway, never()).create(any());
    }

    @Test
    void givenUnknownReferences_whenCallExecute_thenReturnLeftWithOneErrorPerAggregate() {
        final var command = commandWithReferences();
        when(categoryGateway.findExistingIds(any())).thenReturn(Set.of());
        when(genreGateway.findExistingIds(any())).thenReturn(Set.of());
        when(castMemberGateway.findExistingIds(any())).thenReturn(Set.of());

        final var actualResult = useCase.execute(command);

        assertEquals(
                List.of(
                        "Some categories could not be found: " + CATEGORY_ID,
                        "Some genres could not be found: " + GENRE_ID,
                        "Some cast members could not be found: " + MEMBER_ID),
                messagesOf(actualResult.getLeft().getErrors()));
        verify(videoGateway, never()).create(any());
    }

    @Test
    void givenNullTitleAndUnknownCategory_whenCallExecute_thenAccumulateBothErrors() {
        final var command = CreateVideoCommand.with(
                new VideoFields(null, EXPECTED_DESCRIPTION, 2021, 155.0, "12"),
                new VideoReferenceIds(Set.of(CATEGORY_ID), Set.of(), Set.of()));
        when(categoryGateway.findExistingIds(any())).thenReturn(Set.of());

        final var actualResult = useCase.execute(command);

        assertEquals(
                List.of("Some categories could not be found: " + CATEGORY_ID, "'title' should not be null"),
                messagesOf(actualResult.getLeft().getErrors()));
    }

    private static boolean isCreatedFromCommand(final Video video) {
        return EXPECTED_TITLE.equals(video.getTitle())
                && Rating.AGE_12 == video.getRating()
                && video.getCategories().equals(Set.of(CategoryID.from(CATEGORY_ID)))
                && video.getGenres().equals(Set.of(GenreID.from(GENRE_ID)))
                && video.getCastMembers().equals(Set.of(CastMemberID.from(MEMBER_ID)))
                && video.getCreatedAt().equals(video.getUpdatedAt());
    }

    private CreateVideoCommand commandWithReferences() {
        return CreateVideoCommand.with(
                new VideoFields(EXPECTED_TITLE, EXPECTED_DESCRIPTION, 2021, 155.0, "12"),
                new VideoReferenceIds(Set.of(CATEGORY_ID), Set.of(GENRE_ID), Set.of(MEMBER_ID)));
    }

    private List<String> messagesOf(final List<ValidationError> errors) {
        return errors.stream().map(ValidationError::message).toList();
    }
}
