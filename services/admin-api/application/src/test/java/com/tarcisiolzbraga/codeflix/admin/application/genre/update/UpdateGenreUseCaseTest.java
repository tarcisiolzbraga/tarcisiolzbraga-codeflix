package com.tarcisiolzbraga.codeflix.admin.application.genre.update;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateGenreUseCaseTest {

    private static final String EXPECTED_NAME = "Ação";
    private static final String MOVIES_ID = "11111111-1111-1111-1111-111111111111";
    private static final String SERIES_ID = "22222222-2222-2222-2222-222222222222";

    @Mock
    private CategoryGateway categoryGateway;

    @Mock
    private GenreGateway genreGateway;

    @InjectMocks
    private DefaultUpdateGenreUseCase useCase;

    @Test
    void givenValidCommand_whenCallExecute_thenReturnRightWithGenreUpdated() {
        final var genre = givenStoredGenre(true, Set.of());
        final var command = UpdateGenreCommand.with(genre.getId().getValue(), EXPECTED_NAME, Set.of(MOVIES_ID));
        when(categoryGateway.findExistingIds(any())).thenReturn(Set.of(CategoryID.from(MOVIES_ID)));
        when(genreGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        assertEquals(genre.getId().getValue(), actualResult.get().id());
        verify(genreGateway).update(argThat(updated -> isUpdatedTo(updated, true, Set.of(MOVIES_ID))));
    }

    @Test
    void givenCommandWithoutCategories_whenCallExecute_thenClearTheCategories() {
        final var genre = givenStoredGenre(true, Set.of(CategoryID.from(MOVIES_ID)));
        final var command = UpdateGenreCommand.with(genre.getId().getValue(), EXPECTED_NAME, Set.of());
        when(genreGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        verify(categoryGateway, never()).findExistingIds(any());
        verify(genreGateway).update(argThat(updated -> isUpdatedTo(updated, true, Set.of())));
    }

    @Test
    void givenInactiveGenre_whenCallExecute_thenKeepItInactive() {
        final var genre = givenStoredGenre(false, Set.of());
        final var command = UpdateGenreCommand.with(genre.getId().getValue(), EXPECTED_NAME, Set.of());
        when(genreGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        verify(genreGateway).update(argThat(updated -> isUpdatedTo(updated, false, Set.of())));
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFoundAndNotUpdate() {
        final var expectedId = GenreID.unique();
        final var command = UpdateGenreCommand.with(expectedId.getValue(), EXPECTED_NAME, Set.of());
        when(genreGateway.findById(expectedId)).thenReturn(Optional.empty());

        final var actualException = assertThrows(NotFoundException.class, () -> useCase.execute(command));

        assertEquals("Genre with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
        verify(genreGateway, never()).update(any());
    }

    @Test
    void givenNullName_whenCallExecute_thenReturnLeftWithNotificationAndNotUpdate() {
        final var genre = givenStoredGenre(true, Set.of());
        final var command = UpdateGenreCommand.with(genre.getId().getValue(), null, Set.of());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isLeft());
        assertEquals("'name' should not be null", firstErrorOf(actualResult.getLeft()));
        verify(genreGateway, never()).update(any());
    }

    @Test
    void givenUnknownCategories_whenCallExecute_thenReturnLeftListingTheMissingOnes() {
        final var genre = givenStoredGenre(true, Set.of());
        final var command = UpdateGenreCommand
                .with(genre.getId().getValue(), EXPECTED_NAME, Set.of(MOVIES_ID, SERIES_ID));
        when(categoryGateway.findExistingIds(any())).thenReturn(Set.of(CategoryID.from(MOVIES_ID)));

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isLeft());
        assertEquals(
                "Some categories could not be found: %s".formatted(SERIES_ID), firstErrorOf(actualResult.getLeft()));
        verify(genreGateway, never()).update(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var genre = givenStoredGenre(true, Set.of());
        final var command = UpdateGenreCommand.with(genre.getId().getValue(), EXPECTED_NAME, Set.of());
        final var expectedException = new IllegalStateException("gateway indisponível");
        when(genreGateway.update(any())).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        assertSame(expectedException, actualException);
    }

    private String firstErrorOf(final Notification notification) {
        return notification.firstError().orElseThrow().message();
    }

    private Genre givenStoredGenre(final boolean isActive, final Set<CategoryID> categories) {
        final var genre = Genre.newGenre("Acao", isActive).addCategories(categories);
        when(genreGateway.findById(genre.getId())).thenReturn(Optional.of(genre));
        return genre;
    }

    private boolean isUpdatedTo(final Genre genre, final boolean isActive, final Set<String> categories) {
        return EXPECTED_NAME.equals(genre.getName())
                && genre.isActive() == isActive
                && genre.getCategories().equals(categories.stream().map(CategoryID::from).collect(toSet()));
    }
}
