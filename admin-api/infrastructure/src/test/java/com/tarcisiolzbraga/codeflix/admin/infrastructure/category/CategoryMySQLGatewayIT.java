package com.tarcisiolzbraga.codeflix.admin.infrastructure.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class CategoryMySQLGatewayIT {

    @Autowired
    private CategoryMySQLGateway categoryGateway;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void givenCategory_whenCallCreate_thenPersistIt() {
        final var category = Category.newCategory("Filmes", "A mais assistida", true);

        final var actualCategory = this.categoryGateway.create(category);

        assertEquals(category.getId(), actualCategory.getId());
        assertEquals("Filmes", actualCategory.getName());
        assertEquals(1, this.categoryRepository.count());
    }

    @Test
    void givenPersistedCategory_whenCallUpdate_thenSaveTheNewValues() {
        final var category = this.categoryGateway.create(Category.newCategory("Flmes", null, true));

        final var actualCategory = this.categoryGateway.update(category.update("Filmes", "A mais assistida"));

        assertEquals("Filmes", actualCategory.getName());
        assertEquals("A mais assistida", actualCategory.getDescription());
        assertEquals(1, this.categoryRepository.count());
    }

    @Test
    void givenPersistedCategory_whenCallDeleteById_thenRemoveIt() {
        final var category = this.categoryGateway.create(Category.newCategory("Filmes", null, true));

        this.categoryGateway.deleteById(category.getId());

        assertEquals(0, this.categoryRepository.count());
    }

    @Test
    void givenUnknownId_whenCallDeleteById_thenDoNothing() {
        this.categoryGateway.create(Category.newCategory("Filmes", null, true));

        this.categoryGateway.deleteById(CategoryID.from("nao-existe"));

        assertEquals(1, this.categoryRepository.count());
    }

    @Test
    void givenPersistedCategory_whenCallFindById_thenReturnIt() {
        final var category = this.categoryGateway.create(Category.newCategory("Filmes", "A mais assistida", true));

        final var actualCategory = this.categoryGateway.findById(category.getId());

        assertTrue(actualCategory.isPresent());
        assertEquals("Filmes", actualCategory.get().getName());
    }

    @Test
    void givenUnknownId_whenCallFindById_thenReturnEmpty() {
        final var actualCategory = this.categoryGateway.findById(CategoryID.from("nao-existe"));

        assertTrue(actualCategory.isEmpty());
    }

    @Test
    void givenPersistedCategories_whenCallFindAll_thenReturnPaginated() {
        persist("Filmes", "Series", "Documentarios");

        final var actualPage = this.categoryGateway.findAll(new SearchQuery(0, 2, null, "name", "asc"));

        assertEquals(0, actualPage.currentPage());
        assertEquals(2, actualPage.perPage());
        assertEquals(3L, actualPage.total());
        assertEquals(List.of("Documentarios", "Filmes"), namesOf(actualPage.items()));
    }

    @Test
    void givenPersistedCategories_whenCallFindAllWithTerms_thenFilterByNameOrDescription() {
        persist("Filmes", "Series", "Documentarios");

        final var actualPage = this.categoryGateway.findAll(new SearchQuery(0, 10, "doc", "name", "asc"));

        assertEquals(1L, actualPage.total());
        assertEquals(List.of("Documentarios"), namesOf(actualPage.items()));
    }

    @Test
    void givenPersistedCategories_whenCallFindAllOnSecondPage_thenReturnRemainingItems() {
        persist("Filmes", "Series", "Documentarios");

        final var actualPage = this.categoryGateway.findAll(new SearchQuery(1, 2, null, "name", "asc"));

        assertEquals(1, actualPage.currentPage());
        assertEquals(List.of("Series"), namesOf(actualPage.items()));
    }

    @Test
    void givenNoCategory_whenCallFindAll_thenReturnEmptyPage() {
        final var actualPage = this.categoryGateway.findAll(new SearchQuery(0, 10, null, "name", "asc"));

        assertEquals(0L, actualPage.total());
        assertFalse(actualPage.items().iterator().hasNext());
    }

    private void persist(final String... names) {
        for (final var name : names) {
            this.categoryGateway.create(Category.newCategory(name, "descricao de " + name, true));
        }
    }

    private List<String> namesOf(final List<Category> categories) {
        return categories.stream().map(Category::getName).toList();
    }

    @Test
    void givenPersistedCategories_whenCallFindExistingIds_thenReturnOnlyTheExistingOnes() {
        final var movies = this.categoryGateway.create(Category.newCategory("Filmes", null, true));
        final var series = this.categoryGateway.create(Category.newCategory("Series", null, true));
        final var unknown = CategoryID.from("nao-existe");

        final var actualIds = this.categoryGateway
                .findExistingIds(Set.of(movies.getId(), series.getId(), unknown));

        assertEquals(Set.of(movies.getId(), series.getId()), actualIds);
    }

    @Test
    void givenOnlyUnknownIds_whenCallFindExistingIds_thenReturnEmpty() {
        this.categoryGateway.create(Category.newCategory("Filmes", null, true));

        final var actualIds = this.categoryGateway.findExistingIds(Set.of(CategoryID.from("nao-existe")));

        assertTrue(actualIds.isEmpty());
    }

    @Test
    void givenNoIds_whenCallFindExistingIds_thenReturnEmpty() {
        this.categoryGateway.create(Category.newCategory("Filmes", null, true));

        final var actualIds = this.categoryGateway.findExistingIds(Set.of());

        assertTrue(actualIds.isEmpty());
    }
}
