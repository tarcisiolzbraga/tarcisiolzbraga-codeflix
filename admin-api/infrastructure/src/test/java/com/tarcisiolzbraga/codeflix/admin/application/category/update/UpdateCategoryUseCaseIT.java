package com.tarcisiolzbraga.codeflix.admin.application.category.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class UpdateCategoryUseCaseIT {

    private static final String EXPECTED_NAME = "Filmes";
    private static final String EXPECTED_DESCRIPTION = "A categoria mais assistida";

    @Autowired
    private UpdateCategoryUseCase useCase;

    @MockitoSpyBean
    private CategoryGateway categoryGateway;

    @Test
    void givenPersistedCategory_whenCallExecute_thenSaveTheNewValues() {
        final var category = givenPersistedCategory(true);
        final var command =
                UpdateCategoryCommand.with(category.getId().getValue(), EXPECTED_NAME, EXPECTED_DESCRIPTION);

        final var actualResult = this.useCase.execute(command);

        assertTrue(actualResult.isRight());
        final var persisted = reload(category);
        assertEquals(EXPECTED_NAME, persisted.getName());
        assertEquals(EXPECTED_DESCRIPTION, persisted.getDescription());
    }

    @Test
    void givenInactiveCategory_whenCallExecute_thenKeepItInactive() {
        final var category = givenPersistedCategory(false);
        final var command =
                UpdateCategoryCommand.with(category.getId().getValue(), EXPECTED_NAME, EXPECTED_DESCRIPTION);

        this.useCase.execute(command);

        assertEquals(false, reload(category).isActive());
    }

    @Test
    void givenInvalidName_whenCallExecute_thenReturnLeftAndKeepTheStoredValues() {
        final var category = givenPersistedCategory(true);
        final var command = UpdateCategoryCommand.with(category.getId().getValue(), null, EXPECTED_DESCRIPTION);

        final var actualResult = this.useCase.execute(command);

        assertTrue(actualResult.isLeft());
        assertEquals("Série", reload(category).getName());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = CategoryID.unique();
        final var command = UpdateCategoryCommand.with(expectedId.getValue(), EXPECTED_NAME, EXPECTED_DESCRIPTION);

        final var actualException = assertThrows(NotFoundException.class, () -> this.useCase.execute(command));

        assertEquals(
                "Category with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheExceptionInsteadOfValidationError() {
        final var category = givenPersistedCategory(true);
        final var command =
                UpdateCategoryCommand.with(category.getId().getValue(), EXPECTED_NAME, EXPECTED_DESCRIPTION);
        final var expectedException = new IllegalStateException("banco indisponível");
        doThrow(expectedException).when(this.categoryGateway).update(any());

        final var actualException = assertThrows(IllegalStateException.class, () -> this.useCase.execute(command));

        assertSame(expectedException, actualException);
    }

    private Category givenPersistedCategory(final boolean isActive) {
        return this.categoryGateway.create(Category.newCategory("Série", "A descrição antiga", isActive));
    }

    private Category reload(final Category category) {
        return this.categoryGateway.findById(category.getId()).orElseThrow();
    }
}
