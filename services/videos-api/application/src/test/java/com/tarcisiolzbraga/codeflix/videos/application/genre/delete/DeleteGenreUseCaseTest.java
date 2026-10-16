package com.tarcisiolzbraga.codeflix.videos.application.genre.delete;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteGenreUseCaseTest {

    private static final GenreID EXPECTED_ID = GenreID.from("5b8d2e1f-3a4c-4d60-9e71-f2a3b4c5d6e7");

    @Mock
    private GenreGateway genreGateway;

    @InjectMocks
    private DefaultDeleteGenreUseCase useCase;

    @Test
    void givenValidId_whenCallExecute_thenRemoveTheReplica() {
        assertDoesNotThrow(() -> useCase.execute(EXPECTED_ID));

        verify(genreGateway).deleteById(EXPECTED_ID);
    }

    @Test
    void givenTheSameIdTwice_whenCallExecute_thenRemoveTwiceWithoutComplaining() {
        useCase.execute(EXPECTED_ID);
        useCase.execute(EXPECTED_ID);

        verify(genreGateway, times(2)).deleteById(EXPECTED_ID);
    }

    @Test
    void givenNullId_whenCallExecute_thenThrowNullPointerExceptionAndNotTouchTheGateway() {
        final var actualException = assertThrows(NullPointerException.class, () -> useCase.execute(null));

        assertEquals("'input' should not be null", actualException.getMessage());
        verify(genreGateway, never()).deleteById(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedException = new IllegalStateException("catálogo indisponível");
        doThrow(expectedException).when(genreGateway).deleteById(EXPECTED_ID);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(EXPECTED_ID));

        assertSame(expectedException, actualException);
    }
}
