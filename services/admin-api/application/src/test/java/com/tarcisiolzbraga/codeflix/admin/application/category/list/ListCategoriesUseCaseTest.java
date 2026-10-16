package com.tarcisiolzbraga.codeflix.admin.application.category.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListCategoriesUseCaseTest {

    private static final SearchQuery EXPECTED_QUERY = new SearchQuery(0, 10, "fil", "name", "asc");

    @Mock
    private CategoryGateway categoryGateway;

    @InjectMocks
    private DefaultListCategoriesUseCase useCase;

    @Test
    void givenValidQuery_whenCallExecute_thenReturnPaginatedOutputs() {
        final var categories = List.of(
                Category.newCategory("Filmes", "A mais assistida", true),
                Category.newCategory("Séries", "A segunda mais assistida", false));
        when(categoryGateway.findAll(EXPECTED_QUERY)).thenReturn(new Pagination<>(0, 10, 2L, categories));

        final var actualOutput = useCase.execute(EXPECTED_QUERY);

        assertEquals(0, actualOutput.currentPage());
        assertEquals(10, actualOutput.perPage());
        assertEquals(2L, actualOutput.total());
        assertEquals(List.of("Filmes", "Séries"), namesOf(actualOutput));
    }

    @Test
    void givenValidQuery_whenCallExecute_thenMapEachCategoryToOutput() {
        final var category = Category.newCategory("Filmes", "A mais assistida", true);
        when(categoryGateway.findAll(EXPECTED_QUERY)).thenReturn(new Pagination<>(0, 10, 1L, List.of(category)));

        final var actualOutput = useCase.execute(EXPECTED_QUERY);

        final var actualItem = actualOutput.items().getFirst();
        assertEquals(category.getId().getValue(), actualItem.id());
        assertEquals("A mais assistida", actualItem.description());
        assertTrue(actualItem.isActive());
        assertEquals(category.getCreatedAt(), actualItem.createdAt());
    }

    @Test
    void givenQueryWithoutResult_whenCallExecute_thenReturnEmptyPagination() {
        when(categoryGateway.findAll(EXPECTED_QUERY)).thenReturn(new Pagination<>(0, 10, 0L, List.of()));

        final var actualOutput = useCase.execute(EXPECTED_QUERY);

        assertEquals(0L, actualOutput.total());
        assertTrue(actualOutput.items().isEmpty());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedException = new IllegalStateException("gateway indisponível");
        when(categoryGateway.findAll(EXPECTED_QUERY)).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(EXPECTED_QUERY));

        assertSame(expectedException, actualException);
    }

    private List<String> namesOf(final Pagination<CategoryListOutput> pagination) {
        return pagination.items().stream().map(CategoryListOutput::name).toList();
    }
}
