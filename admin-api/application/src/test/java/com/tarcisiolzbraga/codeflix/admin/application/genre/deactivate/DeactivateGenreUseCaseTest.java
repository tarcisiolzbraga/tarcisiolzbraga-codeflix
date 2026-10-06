package com.tarcisiolzbraga.codeflix.admin.application.genre.deactivate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeactivateGenreUseCaseTest {

    private static final String EXPECTED_NAME = "Ação";

    @Mock
    private GenreGateway genreGateway;

    @InjectMocks
    private DefaultDeactivateGenreUseCase useCase;

    @Test
    void givenActiveGenre_whenCallExecute_thenReturnItAsInactive() {
        final var genre = givenStoredGenre(true);
        final var previousUpdatedAt = genre.getUpdatedAt();
        when(genreGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualOutput = useCase.execute(genre.getId().getValue());

        assertEquals(genre.getId().getValue(), actualOutput.id());
        assertEquals(EXPECTED_NAME, actualOutput.name());
        assertFalse(actualOutput.isActive());
        assertTrue(actualOutput.updatedAt().isAfter(previousUpdatedAt));
    }

    @Test
    void givenInactiveGenre_whenCallExecute_thenKeepItAsInactive() {
        final var genre = givenStoredGenre(false);
        when(genreGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualOutput = useCase.execute(genre.getId().getValue());

        assertFalse(actualOutput.isActive());
        verify(genreGateway).update(genre);
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFoundAndNotUpdate() {
        final var expectedId = GenreID.unique();
        when(genreGateway.findById(expectedId)).thenReturn(Optional.empty());

        final var actualException =
                assertThrows(NotFoundException.class, () -> useCase.execute(expectedId.getValue()));

        assertEquals("Genre with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
        verify(genreGateway, never()).update(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var genre = givenStoredGenre(true);
        final var expectedException = new IllegalStateException("gateway indisponível");
        when(genreGateway.update(any())).thenThrow(expectedException);

        final var actualException =
                assertThrows(IllegalStateException.class, () -> useCase.execute(genre.getId().getValue()));

        assertSame(expectedException, actualException);
    }

    private Genre givenStoredGenre(final boolean isActive) {
        final var genre = Genre.newGenre(EXPECTED_NAME, isActive);
        when(genreGateway.findById(genre.getId())).thenReturn(Optional.of(genre));
        return genre;
    }
}
