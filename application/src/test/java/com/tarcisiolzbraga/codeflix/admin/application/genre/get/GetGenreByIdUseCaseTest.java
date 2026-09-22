package com.tarcisiolzbraga.codeflix.admin.application.genre.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetGenreByIdUseCaseTest {

    private static final String EXPECTED_NAME = "Ação";

    @Mock
    private GenreGateway genreGateway;

    @InjectMocks
    private DefaultGetGenreByIdUseCase useCase;

    @Test
    void givenValidId_whenCallExecute_thenReturnTheGenre() {
        final var category = CategoryID.unique();
        final var genre = givenStoredGenre(true, Set.of(category));

        final var actualOutput = useCase.execute(genre.getId().getValue());

        assertEquals(genre.getId().getValue(), actualOutput.id());
        assertEquals(EXPECTED_NAME, actualOutput.name());
        assertTrue(actualOutput.isActive());
        assertEquals(Set.of(category.getValue()), actualOutput.categories());
        assertEquals(genre.getCreatedAt(), actualOutput.createdAt());
        assertEquals(genre.getUpdatedAt(), actualOutput.updatedAt());
    }

    @Test
    void givenInactiveGenre_whenCallExecute_thenReturnItInactive() {
        final var genre = givenStoredGenre(false, Set.of());

        final var actualOutput = useCase.execute(genre.getId().getValue());

        assertFalse(actualOutput.isActive());
        assertTrue(actualOutput.categories().isEmpty());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = GenreID.unique();
        when(genreGateway.findById(expectedId)).thenReturn(Optional.empty());

        final var actualException =
                assertThrows(NotFoundException.class, () -> useCase.execute(expectedId.getValue()));

        assertEquals("Genre with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedId = GenreID.unique();
        final var expectedException = new IllegalStateException("gateway indisponível");
        when(genreGateway.findById(expectedId)).thenThrow(expectedException);

        final var actualException =
                assertThrows(IllegalStateException.class, () -> useCase.execute(expectedId.getValue()));

        assertSame(expectedException, actualException);
    }

    private Genre givenStoredGenre(final boolean isActive, final Set<CategoryID> categories) {
        final var genre = Genre.newGenre(EXPECTED_NAME, isActive).addCategories(categories);
        when(genreGateway.findById(genre.getId())).thenReturn(Optional.of(genre));
        return genre;
    }
}
