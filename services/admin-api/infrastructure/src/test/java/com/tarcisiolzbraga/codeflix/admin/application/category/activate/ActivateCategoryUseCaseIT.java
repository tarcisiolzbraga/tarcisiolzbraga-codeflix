package com.tarcisiolzbraga.codeflix.admin.application.category.activate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ActivateCategoryUseCaseIT {

    @Autowired
    private ActivateCategoryUseCase useCase;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void givenInactiveCategory_whenCallExecute_thenPersistItAsActiveWithoutRemovingIt() {
        final var category = givenPersistedCategory(false);

        final var actualOutput = this.useCase.execute(category.getId().getValue());

        assertTrue(actualOutput.isActive());
        assertTrue(reload(category).isActive());
        assertEquals(1, this.categoryRepository.count());
    }

    @Test
    void givenActiveCategory_whenCallExecute_thenKeepItAsActive() {
        final var category = givenPersistedCategory(true);

        this.useCase.execute(category.getId().getValue());

        assertTrue(reload(category).isActive());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = CategoryID.unique();

        final var actualException =
                assertThrows(NotFoundException.class, () -> this.useCase.execute(expectedId.getValue()));

        assertEquals(
                "Category with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }

    private Category givenPersistedCategory(final boolean isActive) {
        return this.categoryGateway.create(Category.newCategory("Filmes", "A mais assistida", isActive));
    }

    private Category reload(final Category category) {
        return this.categoryGateway.findById(category.getId()).orElseThrow();
    }
}
