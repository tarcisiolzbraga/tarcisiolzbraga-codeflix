package com.tarcisiolzbraga.codeflix.admin.application.category.activate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
class ActivateCategoryUseCaseTest {

    private static final String EXPECTED_NAME = "Filmes";
    private static final String EXPECTED_DESCRIPTION = "A categoria mais assistida";

    @Mock
    private CategoryGateway categoryGateway;

    @InjectMocks
    private DefaultActivateCategoryUseCase useCase;

    @Test
    void givenInactiveCategory_whenCallExecute_thenReturnItAsActive() {
        final var category = givenStoredCategory(false);
        final var previousUpdatedAt = category.getUpdatedAt();
        when(categoryGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualOutput = useCase.execute(category.getId().getValue());

        assertEquals(category.getId().getValue(), actualOutput.id());
        assertEquals(EXPECTED_NAME, actualOutput.name());
        assertTrue(actualOutput.isActive());
        assertTrue(actualOutput.updatedAt().isAfter(previousUpdatedAt));
    }

    @Test
    void givenActiveCategory_whenCallExecute_thenKeepItAsActive() {
        final var category = givenStoredCategory(true);
        when(categoryGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualOutput = useCase.execute(category.getId().getValue());

        assertTrue(actualOutput.isActive());
        verify(categoryGateway).update(category);
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFoundAndNotUpdate() {
        final var expectedId = CategoryID.unique();
        when(categoryGateway.findById(expectedId)).thenReturn(Optional.empty());

        final var actualException =
                assertThrows(NotFoundException.class, () -> useCase.execute(expectedId.getValue()));

        assertEquals(
                "Category with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
        verify(categoryGateway, never()).update(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var category = givenStoredCategory(false);
        final var expectedException = new IllegalStateException("gateway indisponível");
        when(categoryGateway.update(any())).thenThrow(expectedException);

        final var actualException =
                assertThrows(IllegalStateException.class, () -> useCase.execute(category.getId().getValue()));

        assertSame(expectedException, actualException);
    }

    private Category givenStoredCategory(final boolean isActive) {
        final var category = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, isActive);
        when(categoryGateway.findById(category.getId())).thenReturn(Optional.of(category));
        return category;
    }
}
