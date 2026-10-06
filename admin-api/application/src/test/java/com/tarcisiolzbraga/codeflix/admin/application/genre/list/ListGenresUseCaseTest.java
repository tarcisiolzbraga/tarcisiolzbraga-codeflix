package com.tarcisiolzbraga.codeflix.admin.application.genre.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListGenresUseCaseTest {

    private static final SearchQuery EXPECTED_QUERY = new SearchQuery(0, 10, "aca", "name", "asc");

    @Mock
    private GenreGateway genreGateway;

    @InjectMocks
    private DefaultListGenresUseCase useCase;

    @Test
    void givenValidQuery_whenCallExecute_thenReturnPaginatedOutputs() {
        final var genres = List.of(Genre.newGenre("Ação", true), Genre.newGenre("Comédia", false));
        when(genreGateway.findAll(EXPECTED_QUERY)).thenReturn(new Pagination<>(0, 10, 2L, genres));

        final var actualOutput = useCase.execute(EXPECTED_QUERY);

        assertEquals(0, actualOutput.currentPage());
        assertEquals(10, actualOutput.perPage());
        assertEquals(2L, actualOutput.total());
        assertEquals(List.of("Ação", "Comédia"), namesOf(actualOutput));
    }

    @Test
    void givenValidQuery_whenCallExecute_thenMapEachGenreToOutput() {
        final var category = CategoryID.unique();
        final var genre = Genre.newGenre("Ação", true).addCategory(category);
        when(genreGateway.findAll(EXPECTED_QUERY)).thenReturn(new Pagination<>(0, 10, 1L, List.of(genre)));

        final var actualOutput = useCase.execute(EXPECTED_QUERY);

        final var actualItem = actualOutput.items().getFirst();
        assertEquals(genre.getId().getValue(), actualItem.id());
        assertEquals("Ação", actualItem.name());
        assertTrue(actualItem.isActive());
        assertEquals(Set.of(category.getValue()), actualItem.categories());
        assertEquals(genre.getCreatedAt(), actualItem.createdAt());
    }

    @Test
    void givenQueryWithoutResult_whenCallExecute_thenReturnEmptyPagination() {
        when(genreGateway.findAll(EXPECTED_QUERY)).thenReturn(new Pagination<>(0, 10, 0L, List.of()));

        final var actualOutput = useCase.execute(EXPECTED_QUERY);

        assertEquals(0L, actualOutput.total());
        assertTrue(actualOutput.items().isEmpty());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedException = new IllegalStateException("gateway indisponível");
        when(genreGateway.findAll(EXPECTED_QUERY)).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(EXPECTED_QUERY));

        assertSame(expectedException, actualException);
    }

    private List<String> namesOf(final Pagination<GenreListOutput> pagination) {
        return pagination.items().stream().map(GenreListOutput::name).toList();
    }
}
