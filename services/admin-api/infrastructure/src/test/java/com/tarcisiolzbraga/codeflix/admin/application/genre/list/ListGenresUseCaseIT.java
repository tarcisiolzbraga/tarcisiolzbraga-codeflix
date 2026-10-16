package com.tarcisiolzbraga.codeflix.admin.application.genre.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ListGenresUseCaseIT {

    private static final String HORROR = "Terror";
    private static final String ACTION = "Ação";
    private static final String NAME = "name";
    private static final String ASC = "asc";

    @Autowired
    private ListGenresUseCase useCase;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private GenreGateway genreGateway;

    @Test
    void givenPersistedGenres_whenCallExecute_thenReturnThemSortedByNameWithCategories() {
        final var movies = givenPersistedCategory();
        givenPersistedGenre(HORROR, movies);
        givenPersistedGenre(ACTION, movies);

        final var actualPage = this.useCase.execute(new SearchQuery(0, 10, null, NAME, ASC));

        assertEquals(2, actualPage.total());
        assertEquals(ACTION, actualPage.items().getFirst().name());
        assertEquals(HORROR, actualPage.items().getLast().name());
        assertEquals(Set.of(movies.getValue()), actualPage.items().getFirst().categories());
    }

    @Test
    void givenTerms_whenCallExecute_thenFilterByName() {
        final var movies = givenPersistedCategory();
        givenPersistedGenre(HORROR, movies);
        givenPersistedGenre(ACTION, movies);

        final var actualPage = this.useCase.execute(new SearchQuery(0, 10, "ter", NAME, ASC));

        assertEquals(1, actualPage.total());
        assertEquals(HORROR, actualPage.items().getFirst().name());
    }

    @Test
    void givenSecondPage_whenCallExecute_thenReturnTheRemainingItems() {
        final var movies = givenPersistedCategory();
        givenPersistedGenre(HORROR, movies);
        givenPersistedGenre(ACTION, movies);

        final var actualPage = this.useCase.execute(new SearchQuery(1, 1, null, NAME, ASC));

        assertEquals(2, actualPage.total());
        assertEquals(1, actualPage.currentPage());
        assertEquals(HORROR, actualPage.items().getFirst().name());
    }

    @Test
    void givenNoGenre_whenCallExecute_thenReturnEmptyPage() {
        final var actualPage = this.useCase.execute(new SearchQuery(0, 10, null, NAME, ASC));

        assertEquals(0, actualPage.total());
        assertTrue(actualPage.items().isEmpty());
    }

    private CategoryID givenPersistedCategory() {
        return this.categoryGateway.create(Category.newCategory("Filmes", null, true)).getId();
    }

    private void givenPersistedGenre(final String name, final CategoryID category) {
        this.genreGateway.create(Genre.newGenre(name, true).addCategory(category));
    }
}
