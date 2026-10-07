package com.tarcisiolzbraga.codeflix.admin.application.category.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class CreateCategoryUseCaseIT {

    private static final String EXPECTED_NAME = "Filmes";
    private static final String EXPECTED_DESCRIPTION = "A categoria mais assistida";

    @Autowired
    private CreateCategoryUseCase useCase;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoSpyBean
    private CategoryGateway categoryGateway;

    @Test
    void givenValidCommand_whenCallExecute_thenPersistTheCategory() {
        final var command = CreateCategoryCommand.with(EXPECTED_NAME, EXPECTED_DESCRIPTION, true);

        final var actualResult = this.useCase.execute(command);

        assertTrue(actualResult.isRight());
        assertEquals(1, this.categoryRepository.count());
        final var persisted = this.categoryRepository
                .findById(UUID.fromString(actualResult.get().id()))
                .orElseThrow();
        assertEquals(EXPECTED_NAME, persisted.getName());
        assertTrue(persisted.isActive());
    }

    @Test
    void givenInactiveCommand_whenCallExecute_thenPersistItInactive() {
        final var command = CreateCategoryCommand.with(EXPECTED_NAME, EXPECTED_DESCRIPTION, false);

        final var actualResult = this.useCase.execute(command);

        final var persisted = this.categoryRepository
                .findById(UUID.fromString(actualResult.get().id()))
                .orElseThrow();
        assertEquals(false, persisted.isActive());
    }

    @Test
    void givenInvalidName_whenCallExecute_thenReturnLeftAndPersistNothing() {
        final var command = CreateCategoryCommand.with(null, EXPECTED_DESCRIPTION, true);

        final var actualResult = this.useCase.execute(command);

        assertTrue(actualResult.isLeft());
        assertEquals("'name' should not be null", actualResult.getLeft().firstError().orElseThrow().message());
        assertEquals(0, this.categoryRepository.count());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheExceptionInsteadOfValidationError() {
        final var command = CreateCategoryCommand.with(EXPECTED_NAME, EXPECTED_DESCRIPTION, true);
        final var expectedException = new IllegalStateException("banco indisponível");
        doThrow(expectedException).when(this.categoryGateway).create(any());

        final var actualException = assertThrows(IllegalStateException.class, () -> this.useCase.execute(command));

        assertSame(expectedException, actualException);
        assertEquals(0, this.categoryRepository.count());
    }
}
