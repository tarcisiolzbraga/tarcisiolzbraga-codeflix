package com.tarcisiolzbraga.codeflix.admin.application.genre.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class GetGenreByIdUseCaseIT {

    private static final String GENRE_NAME = "Ação";

    @Autowired
    private GetGenreByIdUseCase useCase;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private GenreGateway genreGateway;

    @Test
    void givenPersistedGenre_whenCallExecute_thenReturnItWithCategoriesAndTimestamps() {
        final var movies = this.categoryGateway.create(Category.newCategory("Filmes", null, true)).getId();
        final var genre = this.genreGateway.create(Genre.newGenre(GENRE_NAME, true).addCategory(movies));

        final var actualOutput = this.useCase.execute(genre.getId().getValue());

        assertEquals(genre.getId().getValue(), actualOutput.id());
        assertEquals(GENRE_NAME, actualOutput.name());
        assertTrue(actualOutput.isActive());
        assertEquals(Set.of(movies.getValue()), actualOutput.categories());
        assertEquals(genre.getCreatedAt(), actualOutput.createdAt());
        assertEquals(genre.getUpdatedAt(), actualOutput.updatedAt());
    }

    @Test
    void givenInactiveGenre_whenCallExecute_thenReturnItInactive() {
        final var genre = this.genreGateway.create(Genre.newGenre(GENRE_NAME, false));

        final var actualOutput = this.useCase.execute(genre.getId().getValue());

        assertFalse(actualOutput.isActive());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = GenreID.unique();

        final var actualException =
                assertThrows(NotFoundException.class, () -> this.useCase.execute(expectedId.getValue()));

        assertEquals("Genre with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }
}
