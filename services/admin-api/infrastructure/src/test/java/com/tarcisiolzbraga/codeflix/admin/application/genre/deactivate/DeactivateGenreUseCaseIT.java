package com.tarcisiolzbraga.codeflix.admin.application.genre.deactivate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.persistence.GenreRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class DeactivateGenreUseCaseIT {

    @Autowired
    private DeactivateGenreUseCase useCase;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private GenreGateway genreGateway;

    @Autowired
    private GenreRepository genreRepository;

    @Test
    void givenActiveGenre_whenCallExecute_thenPersistItAsInactiveKeepingTheCategories() {
        final var movies = this.categoryGateway.create(Category.newCategory("Filmes", null, true)).getId();
        final var genre = this.genreGateway.create(Genre.newGenre("Ação", true).addCategory(movies));

        final var actualOutput = this.useCase.execute(genre.getId().getValue());

        assertFalse(actualOutput.isActive());
        final var persisted = reload(genre);
        assertFalse(persisted.isActive());
        assertEquals(Set.of(movies), persisted.getCategories());
        assertEquals(1, this.genreRepository.count());
    }

    @Test
    void givenInactiveGenre_whenCallExecute_thenKeepItAsInactive() {
        final var genre = this.genreGateway.create(Genre.newGenre("Ação", false));

        this.useCase.execute(genre.getId().getValue());

        assertFalse(reload(genre).isActive());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = GenreID.unique();

        final var actualException =
                assertThrows(NotFoundException.class, () -> this.useCase.execute(expectedId.getValue()));

        assertEquals("Genre with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }

    private Genre reload(final Genre genre) {
        return this.genreGateway.findById(genre.getId()).orElseThrow();
    }
}
