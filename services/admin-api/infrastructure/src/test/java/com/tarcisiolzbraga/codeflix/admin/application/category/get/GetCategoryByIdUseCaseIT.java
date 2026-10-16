package com.tarcisiolzbraga.codeflix.admin.application.category.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class GetCategoryByIdUseCaseIT {

    @Autowired
    private GetCategoryByIdUseCase useCase;

    @Autowired
    private CategoryGateway categoryGateway;

    @Test
    void givenPersistedCategory_whenCallExecute_thenReturnItWithTheStoredTimestamps() {
        final var category = givenPersistedCategory(true);

        final var actualOutput = this.useCase.execute(category.getId().getValue());

        assertEquals(category.getId().getValue(), actualOutput.id());
        assertEquals("Filmes", actualOutput.name());
        assertEquals("A mais assistida", actualOutput.description());
        assertTrue(actualOutput.isActive());
        assertEquals(category.getCreatedAt(), actualOutput.createdAt());
    }

    @Test
    void givenInactiveCategory_whenCallExecute_thenReturnItInactive() {
        final var category = givenPersistedCategory(false);

        final var actualOutput = this.useCase.execute(category.getId().getValue());

        assertFalse(actualOutput.isActive());
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
}
