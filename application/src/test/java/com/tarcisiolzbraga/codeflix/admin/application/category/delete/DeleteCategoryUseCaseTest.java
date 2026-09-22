package com.tarcisiolzbraga.codeflix.admin.application.category.delete;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteCategoryUseCaseTest {

    @Mock
    private CategoryGateway categoryGateway;

    @Mock
    private GenreGateway genreGateway;

    @InjectMocks
    private DefaultDeleteCategoryUseCase useCase;

    @Test
    void givenValidId_whenCallExecute_thenDeleteTheCategory() {
        final var expectedId = CategoryID.unique();
        when(genreGateway.existsByCategory(expectedId)).thenReturn(false);
        doNothing().when(categoryGateway).deleteById(expectedId);

        useCase.execute(expectedId.getValue());

        verify(categoryGateway).deleteById(expectedId);
    }

    @Test
    void givenCategoryLinkedToGenre_whenCallExecute_thenThrowConflictAndKeepIt() {
        final var expectedId = CategoryID.unique();
        when(genreGateway.existsByCategory(expectedId)).thenReturn(true);

        final var actualException =
                assertThrows(ConflictException.class, () -> useCase.execute(expectedId.getValue()));

        assertEquals(
                "Category with ID %s is linked to at least one Genre; deactivate it instead"
                        .formatted(expectedId.getValue()),
                actualException.getMessage());
        verify(categoryGateway, never()).deleteById(any());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenDoNotThrow() {
        final var expectedId = CategoryID.unique();
        when(genreGateway.existsByCategory(expectedId)).thenReturn(false);
        doNothing().when(categoryGateway).deleteById(expectedId);

        assertDoesNotThrow(() -> useCase.execute(expectedId.getValue()));

        verify(categoryGateway).deleteById(expectedId);
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedId = CategoryID.unique();
        final var expectedException = new IllegalStateException("gateway indisponível");
        when(genreGateway.existsByCategory(expectedId)).thenReturn(false);
        doThrow(expectedException).when(categoryGateway).deleteById(expectedId);

        final var actualException =
                assertThrows(IllegalStateException.class, () -> useCase.execute(expectedId.getValue()));

        assertSame(expectedException, actualException);
    }
}
