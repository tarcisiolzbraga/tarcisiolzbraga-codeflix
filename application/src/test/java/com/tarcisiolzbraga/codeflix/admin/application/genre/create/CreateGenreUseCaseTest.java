package com.tarcisiolzbraga.codeflix.admin.application.genre.create;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateGenreUseCaseTest {

    private static final String EXPECTED_NAME = "Ação";
    private static final String MOVIES_ID = "aaa-filmes";
    private static final String SERIES_ID = "bbb-series";

    @Mock
    private CategoryGateway categoryGateway;

    @Mock
    private GenreGateway genreGateway;

    @InjectMocks
    private DefaultCreateGenreUseCase useCase;

    @Test
    void givenValidCommand_whenCallExecute_thenReturnRightWithGenreId() {
        final var command = CreateGenreCommand.with(EXPECTED_NAME, true, Set.of(MOVIES_ID));
        when(categoryGateway.findExistingIds(any())).thenReturn(Set.of(CategoryID.from(MOVIES_ID)));
        when(genreGateway.create(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        assertNotNull(actualResult.get().id());
        verify(genreGateway).create(argThat(genre -> isCreatedFrom(genre, true, Set.of(MOVIES_ID))));
    }

    @Test
    void givenCommandWithoutCategories_whenCallExecute_thenNotQueryTheCategories() {
        final var command = CreateGenreCommand.with(EXPECTED_NAME, true, Set.of());
        when(genreGateway.create(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        verify(categoryGateway, never()).findExistingIds(any());
        verify(genreGateway).create(argThat(genre -> isCreatedFrom(genre, true, Set.of())));
    }

    @Test
    void givenInactiveCommand_whenCallExecute_thenCreateInactiveGenre() {
        final var command = CreateGenreCommand.with(EXPECTED_NAME, false, Set.of());
        when(genreGateway.create(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        verify(genreGateway).create(argThat(genre -> isCreatedFrom(genre, false, Set.of())));
    }

    @Test
    void givenNullCategories_whenCallExecute_thenCreateGenreWithoutCategories() {
        final var command = CreateGenreCommand.with(EXPECTED_NAME, true, null);
        when(genreGateway.create(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        verify(genreGateway).create(argThat(genre -> isCreatedFrom(genre, true, Set.of())));
    }

    @Test
    void givenNullName_whenCallExecute_thenReturnLeftWithNotificationAndNotCreate() {
        final var command = CreateGenreCommand.with(null, true, Set.of());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isLeft());
        assertEquals(1, actualResult.getLeft().getErrors().size());
        assertEquals("'name' should not be null", firstErrorOf(actualResult.getLeft()));
        verify(genreGateway, never()).create(any());
    }

    @Test
    void givenBlankName_whenCallExecute_thenReturnLeftWithNotificationAndNotCreate() {
        final var command = CreateGenreCommand.with("   ", true, Set.of());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isLeft());
        assertEquals("'name' should not be empty", firstErrorOf(actualResult.getLeft()));
        verify(genreGateway, never()).create(any());
    }

    @Test
    void givenUnknownCategories_whenCallExecute_thenReturnLeftListingTheMissingOnes() {
        final var command = CreateGenreCommand.with(EXPECTED_NAME, true, Set.of(MOVIES_ID, SERIES_ID));
        when(categoryGateway.findExistingIds(any())).thenReturn(Set.of());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isLeft());
        assertEquals(
                "Some categories could not be found: %s, %s".formatted(MOVIES_ID, SERIES_ID),
                firstErrorOf(actualResult.getLeft()));
        verify(genreGateway, never()).create(any());
    }

    @Test
    void givenUnknownCategoryAndInvalidName_whenCallExecute_thenReturnLeftWithBothErrors() {
        final var command = CreateGenreCommand.with(null, true, Set.of(MOVIES_ID));
        when(categoryGateway.findExistingIds(any())).thenReturn(Set.of());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isLeft());
        assertEquals(2, actualResult.getLeft().getErrors().size());
        verify(genreGateway, never()).create(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var command = CreateGenreCommand.with(EXPECTED_NAME, true, Set.of());
        final var expectedException = new IllegalStateException("gateway indisponível");
        when(genreGateway.create(any())).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        assertSame(expectedException, actualException);
    }

    private String firstErrorOf(final Notification notification) {
        return notification.firstError().orElseThrow().message();
    }

    private boolean isCreatedFrom(final Genre genre, final boolean isActive, final Set<String> categories) {
        return EXPECTED_NAME.equals(genre.getName())
                && genre.isActive() == isActive
                && genre.getCategories().equals(categories.stream().map(CategoryID::from).collect(toSet()))
                && genre.getId() != null
                && genre.getCreatedAt() != null
                && genre.getUpdatedAt() != null;
    }
}
