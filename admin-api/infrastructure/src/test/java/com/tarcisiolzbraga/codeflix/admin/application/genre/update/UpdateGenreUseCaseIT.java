package com.tarcisiolzbraga.codeflix.admin.application.genre.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class UpdateGenreUseCaseIT {

    private static final String MOVIES = "Filmes";
    private static final String OLD_NAME = "Acao";
    private static final String EXPECTED_NAME = "Ação";

    @Autowired
    private UpdateGenreUseCase useCase;

    @Autowired
    private CategoryGateway categoryGateway;

    @MockitoSpyBean
    private GenreGateway genreGateway;

    @Test
    void givenPersistedGenre_whenCallExecute_thenReplaceNameAndCategories() {
        final var genre = givenPersistedGenre(true, givenPersistedCategory(MOVIES));
        final var series = givenPersistedCategory("Séries");
        final var command = UpdateGenreCommand.with(genre.getId().getValue(), EXPECTED_NAME, Set.of(series.getValue()));

        final var actualResult = this.useCase.execute(command);

        assertTrue(actualResult.isRight());
        final var persisted = reload(genre);
        assertEquals(EXPECTED_NAME, persisted.getName());
        assertEquals(Set.of(series), persisted.getCategories());
    }

    @Test
    void givenInactiveGenre_whenCallExecute_thenKeepItInactive() {
        final var genre = givenPersistedGenre(false, givenPersistedCategory(MOVIES));
        final var command = UpdateGenreCommand.with(genre.getId().getValue(), EXPECTED_NAME, null);

        this.useCase.execute(command);

        assertFalse(reload(genre).isActive());
    }

    @Test
    void givenPersistedGenre_whenCallExecute_thenKeepCreatedAtAndAdvanceUpdatedAt() {
        final var genre = givenPersistedGenre(true, givenPersistedCategory(MOVIES));
        final var command = UpdateGenreCommand.with(genre.getId().getValue(), EXPECTED_NAME, null);

        this.useCase.execute(command);

        final var persisted = reload(genre);
        assertEquals(genre.getCreatedAt(), persisted.getCreatedAt());
        assertTrue(genre.getUpdatedAt().isBefore(persisted.getUpdatedAt()));
    }

    @Test
    void givenInvalidNameAndUnknownCategory_whenCallExecute_thenReturnBothErrorsAndKeepTheStoredValues() {
        final var movies = givenPersistedCategory(MOVIES);
        final var genre = givenPersistedGenre(true, movies);
        final var unknown = CategoryID.unique().getValue();
        final var command = UpdateGenreCommand.with(genre.getId().getValue(), null, Set.of(unknown));

        final var actualResult = this.useCase.execute(command);

        assertEquals(
                List.of("Some categories could not be found: " + unknown, "'name' should not be null"),
                actualResult.getLeft().getErrors().stream().map(ValidationError::message).toList());
        assertEquals(OLD_NAME, reload(genre).getName());
        assertEquals(Set.of(movies), reload(genre).getCategories());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = GenreID.unique();
        final var command = UpdateGenreCommand.with(expectedId.getValue(), EXPECTED_NAME, null);

        final var actualException = assertThrows(NotFoundException.class, () -> this.useCase.execute(command));

        assertEquals("Genre with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheExceptionInsteadOfValidationError() {
        final var genre = givenPersistedGenre(true, givenPersistedCategory(MOVIES));
        final var command = UpdateGenreCommand.with(genre.getId().getValue(), EXPECTED_NAME, null);
        final var expectedException = new IllegalStateException("banco indisponível");
        doThrow(expectedException).when(this.genreGateway).update(any());

        final var actualException = assertThrows(IllegalStateException.class, () -> this.useCase.execute(command));

        assertSame(expectedException, actualException);
    }

    private CategoryID givenPersistedCategory(final String name) {
        return this.categoryGateway.create(Category.newCategory(name, null, true)).getId();
    }

    private Genre givenPersistedGenre(final boolean isActive, final CategoryID category) {
        return this.genreGateway.create(Genre.newGenre(OLD_NAME, isActive).addCategory(category));
    }

    private Genre reload(final Genre genre) {
        return this.genreGateway.findById(genre.getId()).orElseThrow();
    }
}
