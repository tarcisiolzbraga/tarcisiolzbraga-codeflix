package com.tarcisiolzbraga.codeflix.videos.application.genre.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.application.genre.GenreOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetGenresByIdUseCaseTest {

    private static final GenreID FIRST_ID = GenreID.from("00000001-0000-0000-0000-000000000000");
    private static final GenreID SECOND_ID = GenreID.from("00000002-0000-0000-0000-000000000000");
    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    @Mock
    private GenreGateway genreGateway;

    @InjectMocks
    private DefaultGetGenresByIdUseCase useCase;

    @Test
    void givenKnownIds_whenCallExecute_thenReturnOneOutputPerGenreFound() {
        final var ids = Set.of(FIRST_ID, SECOND_ID);
        when(genreGateway.findAllById(ids))
                .thenReturn(List.of(aGenre(FIRST_ID, "Ação"), aGenre(SECOND_ID, "Comédia")));

        final var actualOutput = useCase.execute(ids);

        assertEquals(2, actualOutput.size());
        assertEquals(List.of("00000001-0000-0000-0000-000000000000", "00000002-0000-0000-0000-000000000000"), actualOutput.stream().map(GenreOutput::id).toList());
    }

    @Test
    void givenNoId_whenCallExecute_thenReturnEmptyWithoutTouchingTheGateway() {
        final var actualOutput = useCase.execute(Set.of());

        assertTrue(actualOutput.isEmpty());
        verify(genreGateway, never()).findAllById(any());
    }

    @Test
    void givenIdsThatTheCatalogDoesNotHave_whenCallExecute_thenReturnOnlyWhatWasFound() {
        final var ids = Set.of(FIRST_ID, SECOND_ID);
        when(genreGateway.findAllById(ids)).thenReturn(List.of(aGenre(FIRST_ID, "Ação")));

        final var actualOutput = useCase.execute(ids);

        assertEquals(1, actualOutput.size());
        assertEquals("00000001-0000-0000-0000-000000000000", actualOutput.getFirst().id());
    }

    @Test
    void givenNullInput_whenCallExecute_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> useCase.execute(null));

        assertEquals("'input' should not be null", actualException.getMessage());
        verify(genreGateway, never()).findAllById(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var ids = Set.of(FIRST_ID);
        final var expectedException = new IllegalStateException("catálogo indisponível");
        when(genreGateway.findAllById(ids)).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(ids));

        assertSame(expectedException, actualException);
    }

    private static Genre aGenre(final GenreID id, final String name) {
        return Genre.with(id, name, true, Set.of(CategoryID.from("00000009-0000-0000-0000-000000000000")), CREATED_AT, UPDATED_AT);
    }
}
