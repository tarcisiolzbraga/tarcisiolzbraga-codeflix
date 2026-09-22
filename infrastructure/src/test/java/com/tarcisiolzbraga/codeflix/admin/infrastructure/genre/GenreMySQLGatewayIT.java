package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.CategoryMySQLGateway;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.persistence.GenreRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class GenreMySQLGatewayIT {

    private static final String NAME = "name";
    private static final String ASC = "asc";

    @Autowired
    private GenreMySQLGateway genreGateway;

    @Autowired
    private CategoryMySQLGateway categoryGateway;

    @Autowired
    private GenreRepository genreRepository;

    @Test
    void givenGenreWithCategories_whenCallCreate_thenPersistIt() {
        final var movies = existingCategory("Filmes");
        final var genre = Genre.newGenre("Ação", true).addCategory(movies);

        final var actualGenre = this.genreGateway.create(genre);

        assertEquals(genre.getId(), actualGenre.getId());
        assertEquals("Ação", actualGenre.getName());
        assertEquals(Set.of(movies), actualGenre.getCategories());
        assertEquals(1, this.genreRepository.count());
    }

    @Test
    void givenPersistedGenre_whenCallUpdate_thenReplaceNameAndCategories() {
        final var movies = existingCategory("Filmes");
        final var series = existingCategory("Séries");
        final var genre = this.genreGateway.create(Genre.newGenre("Acao", true).addCategory(movies));

        final var actualGenre = this.genreGateway.update(genre.update("Ação", Set.of(series)));

        assertEquals("Ação", actualGenre.getName());
        assertEquals(Set.of(series), this.genreGateway.findById(genre.getId()).orElseThrow().getCategories());
        assertEquals(1, this.genreRepository.count());
    }

    @Test
    void givenPersistedGenreWithCategories_whenCallDeleteById_thenRemoveIt() {
        final var genre = this.genreGateway.create(Genre.newGenre("Ação", true).addCategory(existingCategory("Filmes")));

        this.genreGateway.deleteById(genre.getId());

        assertEquals(0, this.genreRepository.count());
    }

    @Test
    void givenUnknownId_whenCallDeleteById_thenDoNothing() {
        this.genreGateway.create(Genre.newGenre("Ação", true));

        this.genreGateway.deleteById(GenreID.from("nao-existe"));

        assertEquals(1, this.genreRepository.count());
    }

    @Test
    void givenPersistedGenre_whenCallFindById_thenReturnItWithCategories() {
        final var movies = existingCategory("Filmes");
        final var genre = this.genreGateway.create(Genre.newGenre("Ação", false).addCategory(movies));

        final var actualGenre = this.genreGateway.findById(genre.getId()).orElseThrow();

        assertEquals("Ação", actualGenre.getName());
        assertEquals(Set.of(movies), actualGenre.getCategories());
        assertFalse(actualGenre.isActive());
    }

    @Test
    void givenUnknownId_whenCallFindById_thenReturnEmpty() {
        final var actualGenre = this.genreGateway.findById(GenreID.from("nao-existe"));

        assertTrue(actualGenre.isEmpty());
    }

    @Test
    void givenPersistedGenres_whenCallFindAll_thenReturnPaginatedWithCategories() {
        final var movies = existingCategory("Filmes");
        persist(movies, "Drama", "Ação", "Terror");

        final var actualPage = this.genreGateway.findAll(new SearchQuery(0, 2, null, NAME, ASC));

        assertEquals(0, actualPage.currentPage());
        assertEquals(2, actualPage.perPage());
        assertEquals(3L, actualPage.total());
        assertEquals(List.of("Ação", "Drama"), namesOf(actualPage.items()));
        assertEquals(Set.of(movies), actualPage.items().getFirst().getCategories());
    }

    @Test
    void givenPersistedGenres_whenCallFindAllWithTerms_thenFilterByName() {
        persist(existingCategory("Filmes"), "Drama", "Ação", "Terror");

        final var actualPage = this.genreGateway.findAll(new SearchQuery(0, 10, "ter", NAME, ASC));

        assertEquals(1L, actualPage.total());
        assertEquals(List.of("Terror"), namesOf(actualPage.items()));
    }

    @Test
    void givenPersistedGenres_whenCallFindAllOnSecondPage_thenReturnRemainingItems() {
        persist(existingCategory("Filmes"), "Drama", "Ação", "Terror");

        final var actualPage = this.genreGateway.findAll(new SearchQuery(1, 2, null, NAME, ASC));

        assertEquals(1, actualPage.currentPage());
        assertEquals(List.of("Terror"), namesOf(actualPage.items()));
    }

    @Test
    void givenNoGenre_whenCallFindAll_thenReturnEmptyPage() {
        final var actualPage = this.genreGateway.findAll(new SearchQuery(0, 10, null, NAME, ASC));

        assertEquals(0L, actualPage.total());
        assertTrue(actualPage.items().isEmpty());
    }

    @Test
    void givenCategoryLinkedToGenre_whenCallExistsByCategory_thenReturnTrue() {
        final var movies = existingCategory("Filmes");
        persist(movies, "Drama");

        final var actualResult = this.genreGateway.existsByCategory(movies);

        assertTrue(actualResult);
    }

    @Test
    void givenCategoryWithoutGenres_whenCallExistsByCategory_thenReturnFalse() {
        persist(existingCategory("Filmes"), "Drama");
        final var series = existingCategory("Séries");

        final var actualResult = this.genreGateway.existsByCategory(series);

        assertFalse(actualResult);
    }

    private CategoryID existingCategory(final String name) {
        return this.categoryGateway.create(Category.newCategory(name, null, true)).getId();
    }

    private void persist(final CategoryID category, final String... names) {
        for (final var name : names) {
            this.genreGateway.create(Genre.newGenre(name, true).addCategory(category));
        }
    }

    private List<String> namesOf(final List<Genre> genres) {
        return genres.stream().map(Genre::getName).toList();
    }
}
