package com.tarcisiolzbraga.codeflix.admin.application.category.deactivate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class DeactivateCategoryUseCaseIT {

    @Autowired
    private DeactivateCategoryUseCase useCase;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanUp() {
        this.categoryRepository.deleteAll();
    }

    @Test
    void givenActiveCategory_whenCallExecute_thenPersistItAsInactiveWithoutRemovingIt() {
        final var category = givenPersistedCategory(true);

        final var actualOutput = this.useCase.execute(category.getId().getValue());

        assertFalse(actualOutput.isActive());
        assertFalse(reload(category).isActive());
        assertEquals(1, this.categoryRepository.count());
    }

    @Test
    void givenInactiveCategory_whenCallExecute_thenKeepItAsInactive() {
        final var category = givenPersistedCategory(false);

        this.useCase.execute(category.getId().getValue());

        assertFalse(reload(category).isActive());
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
