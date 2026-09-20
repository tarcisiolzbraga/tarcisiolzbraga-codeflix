package com.tarcisiolzbraga.codeflix.admin.application.category.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ListCategoriesUseCaseIT {

    @Autowired
    private ListCategoriesUseCase useCase;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanUp() {
        this.categoryRepository.deleteAll();
    }

    @Test
    void givenPersistedCategories_whenCallExecute_thenReturnThemSortedByName() {
        givenPersistedCategory("Séries", "assistidas em casa");
        givenPersistedCategory("Filmes", "a mais assistida");

        final var actualPage = this.useCase.execute(new SearchQuery(0, 10, null, "name", "asc"));

        assertEquals(2, actualPage.total());
        assertEquals("Filmes", actualPage.items().getFirst().name());
        assertEquals("Séries", actualPage.items().getLast().name());
    }

    @Test
    void givenTerms_whenCallExecute_thenFilterByNameOrDescription() {
        givenPersistedCategory("Filmes", "a mais assistida");
        givenPersistedCategory("Séries", "assistidas em casa");

        final var actualPage = this.useCase.execute(new SearchQuery(0, 10, "casa", "name", "asc"));

        assertEquals(1, actualPage.total());
        assertEquals("Séries", actualPage.items().getFirst().name());
    }

    @Test
    void givenSecondPage_whenCallExecute_thenReturnTheRemainingItems() {
        givenPersistedCategory("Filmes", "a mais assistida");
        givenPersistedCategory("Séries", "assistidas em casa");

        final var actualPage = this.useCase.execute(new SearchQuery(1, 1, null, "name", "asc"));

        assertEquals(2, actualPage.total());
        assertEquals(1, actualPage.currentPage());
        assertEquals("Séries", actualPage.items().getFirst().name());
    }

    @Test
    void givenNoCategory_whenCallExecute_thenReturnEmptyPage() {
        final var actualPage = this.useCase.execute(new SearchQuery(0, 10, null, "name", "asc"));

        assertEquals(0, actualPage.total());
        assertTrue(actualPage.items().isEmpty());
    }

    private Category givenPersistedCategory(final String name, final String description) {
        return this.categoryGateway.create(Category.newCategory(name, description, true));
    }
}
