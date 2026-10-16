package com.tarcisiolzbraga.codeflix.videos.application.category.delete;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteCategoryUseCaseTest {

    private static final CategoryID EXPECTED_ID = CategoryID.from("3f2b1a9c-5d6e-4f70-8a91-b2c3d4e5f607");

    @Mock
    private CategoryGateway categoryGateway;

    @InjectMocks
    private DefaultDeleteCategoryUseCase useCase;

    @Test
    void givenValidId_whenCallExecute_thenRemoveTheReplica() {
        assertDoesNotThrow(() -> useCase.execute(EXPECTED_ID));

        verify(categoryGateway).deleteById(EXPECTED_ID);
    }

    @Test
    void givenTheSameIdTwice_whenCallExecute_thenRemoveTwiceWithoutComplaining() {
        useCase.execute(EXPECTED_ID);
        useCase.execute(EXPECTED_ID);

        verify(categoryGateway, times(2)).deleteById(EXPECTED_ID);
    }

    @Test
    void givenNullId_whenCallExecute_thenThrowNullPointerExceptionAndNotTouchTheGateway() {
        final var actualException = assertThrows(NullPointerException.class, () -> useCase.execute(null));

        assertEquals("'input' should not be null", actualException.getMessage());
        verify(categoryGateway, never()).deleteById(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedException = new IllegalStateException("catálogo indisponível");
        doThrow(expectedException).when(categoryGateway).deleteById(EXPECTED_ID);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(EXPECTED_ID));

        assertSame(expectedException, actualException);
    }
}
