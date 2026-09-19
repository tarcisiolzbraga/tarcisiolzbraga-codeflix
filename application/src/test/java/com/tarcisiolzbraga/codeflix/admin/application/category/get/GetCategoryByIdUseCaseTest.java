package com.tarcisiolzbraga.codeflix.admin.application.category.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCategoryByIdUseCaseTest {

    private static final String EXPECTED_NAME = "Filmes";
    private static final String EXPECTED_DESCRIPTION = "A categoria mais assistida";

    @Mock
    private CategoryGateway categoryGateway;

    @InjectMocks
    private DefaultGetCategoryByIdUseCase useCase;

    @Test
    void givenValidId_whenCallExecute_thenReturnTheCategory() {
        final var category = givenStoredCategory(true);

        final var actualOutput = useCase.execute(category.getId().getValue());

        assertEquals(category.getId().getValue(), actualOutput.id());
        assertEquals(EXPECTED_NAME, actualOutput.name());
        assertEquals(EXPECTED_DESCRIPTION, actualOutput.description());
        assertTrue(actualOutput.isActive());
        assertEquals(category.getCreatedAt(), actualOutput.createdAt());
        assertEquals(category.getUpdatedAt(), actualOutput.updatedAt());
        assertNull(actualOutput.deletedAt());
    }

    @Test
    void givenInactiveCategory_whenCallExecute_thenReturnItWithDeletedAt() {
        final var category = givenStoredCategory(false);

        final var actualOutput = useCase.execute(category.getId().getValue());

        assertEquals(category.getId().getValue(), actualOutput.id());
        assertEquals(false, actualOutput.isActive());
        assertNotNull(actualOutput.deletedAt());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = CategoryID.unique();
        when(categoryGateway.findById(expectedId)).thenReturn(Optional.empty());

        final var actualException =
                assertThrows(NotFoundException.class, () -> useCase.execute(expectedId.getValue()));

        assertEquals(
                "Category with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedId = CategoryID.unique();
        final var expectedException = new IllegalStateException("gateway indisponível");
        when(categoryGateway.findById(expectedId)).thenThrow(expectedException);

        final var actualException =
                assertThrows(IllegalStateException.class, () -> useCase.execute(expectedId.getValue()));

        assertSame(expectedException, actualException);
    }

    private Category givenStoredCategory(final boolean isActive) {
        final var category = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, isActive);
        when(categoryGateway.findById(category.getId())).thenReturn(Optional.of(category));
        return category;
    }
}
