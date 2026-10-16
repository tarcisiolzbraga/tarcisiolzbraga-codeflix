package com.tarcisiolzbraga.codeflix.videos.application.genre.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.application.genre.GenreOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreSearchQuery;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListGenresUseCaseTest {

    private static final GenreSearchQuery EXPECTED_QUERY =
            new GenreSearchQuery(0, 10, "aç", "name", "asc", Set.of(CategoryID.from("00000009-0000-0000-0000-000000000000")));
    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    @Mock
    private GenreGateway genreGateway;

    @InjectMocks
    private DefaultListGenresUseCase useCase;

    @Test
    void givenValidQuery_whenCallExecute_thenReturnThePageWithItsMetadata() {
        when(genreGateway.findAll(EXPECTED_QUERY))
                .thenReturn(new Pagination<>(0, 10, 2L, List.of(aGenre("00000001-0000-0000-0000-000000000000", "Ação"), aGenre("00000002-0000-0000-0000-000000000000", "Comédia"))));

        final var actualOutput = useCase.execute(EXPECTED_QUERY);

        assertEquals(2L, actualOutput.total());
        assertEquals(
                List.of("Ação", "Comédia"),
                actualOutput.items().stream().map(GenreOutput::name).toList());
    }

    @Test
    void givenValidQuery_whenCallExecute_thenCarryTheCategoriesOfEachGenre() {
        when(genreGateway.findAll(EXPECTED_QUERY))
                .thenReturn(new Pagination<>(0, 10, 1L, List.of(aGenre("00000001-0000-0000-0000-000000000000", "Ação"))));

        final var actualOutput = useCase.execute(EXPECTED_QUERY);

        assertEquals(
                Set.of(CategoryID.from("00000009-0000-0000-0000-000000000000"), CategoryID.from("00000010-0000-0000-0000-000000000000")),
                actualOutput.items().getFirst().categories());
    }

    @Test
    void givenQueryWithoutResult_whenCallExecute_thenReturnAnEmptyPage() {
        when(genreGateway.findAll(EXPECTED_QUERY)).thenReturn(new Pagination<>(0, 10, 0L, List.of()));

        final var actualOutput = useCase.execute(EXPECTED_QUERY);

        assertEquals(0L, actualOutput.total());
        assertTrue(actualOutput.items().isEmpty());
    }

    @Test
    void givenNullQuery_whenCallExecute_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> useCase.execute(null));

        assertEquals("'input' should not be null", actualException.getMessage());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedException = new IllegalStateException("catálogo indisponível");
        when(genreGateway.findAll(EXPECTED_QUERY)).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(EXPECTED_QUERY));

        assertSame(expectedException, actualException);
    }

    private static Genre aGenre(final String id, final String name) {
        return Genre.with(
                GenreID.from(id),
                name,
                true,
                Set.of(CategoryID.from("00000009-0000-0000-0000-000000000000"), CategoryID.from("00000010-0000-0000-0000-000000000000")),
                CREATED_AT,
                UPDATED_AT);
    }
}
