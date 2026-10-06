package com.tarcisiolzbraga.codeflix.admin.application.genre.delete;

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

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteGenreUseCaseTest {

    @Mock
    private GenreGateway genreGateway;

    @Mock
    private VideoGateway videoGateway;

    @InjectMocks
    private DefaultDeleteGenreUseCase useCase;

    @Test
    void givenValidId_whenCallExecute_thenDeleteTheGenre() {
        final var expectedId = GenreID.unique();
        doNothing().when(genreGateway).deleteById(expectedId);

        useCase.execute(expectedId.getValue());

        verify(genreGateway).deleteById(expectedId);
    }

    @Test
    void givenUnknownId_whenCallExecute_thenDoNotThrow() {
        final var expectedId = GenreID.unique();
        doNothing().when(genreGateway).deleteById(expectedId);

        assertDoesNotThrow(() -> useCase.execute(expectedId.getValue()));

        verify(genreGateway).deleteById(expectedId);
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedId = GenreID.unique();
        final var expectedException = new IllegalStateException("gateway indisponível");
        doThrow(expectedException).when(genreGateway).deleteById(expectedId);

        final var actualException =
                assertThrows(IllegalStateException.class, () -> useCase.execute(expectedId.getValue()));

        assertSame(expectedException, actualException);
    }
    @Test
    void givenGenreLinkedToAVideo_whenCallExecute_thenThrowConflictAndKeepIt() {
        final var expectedId = GenreID.unique();
        when(videoGateway.existsByGenre(expectedId)).thenReturn(true);

        final var actualException =
                assertThrows(ConflictException.class, () -> useCase.execute(expectedId.getValue()));

        assertEquals(
                "Genre with ID %s is linked to at least one Video; deactivate it instead"
                        .formatted(expectedId.getValue()),
                actualException.getMessage());
        verify(genreGateway, never()).deleteById(any());
    }
}
