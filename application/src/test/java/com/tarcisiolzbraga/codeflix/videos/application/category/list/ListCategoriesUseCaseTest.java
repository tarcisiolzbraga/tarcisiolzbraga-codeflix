package com.tarcisiolzbraga.codeflix.videos.application.category.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.application.category.CategoryOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.category.Category;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.SearchQuery;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListCategoriesUseCaseTest {

    private static final SearchQuery EXPECTED_QUERY = new SearchQuery(0, 10, "fil", "name", "asc");
    private static final Instant EXPECTED_CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant EXPECTED_UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    @Mock
    private CategoryGateway categoryGateway;

    @InjectMocks
    private DefaultListCategoriesUseCase useCase;

    @Test
    void givenValidQuery_whenCallExecute_thenReturnThePageWithItsMetadata() {
        final var categories = List.of(aCategory("1", "Filmes"), aCategory("2", "Séries"));
        when(categoryGateway.findAll(EXPECTED_QUERY)).thenReturn(new Pagination<>(0, 10, 2L, categories));

        final var actualOutput = useCase.execute(EXPECTED_QUERY);

        assertEquals(0, actualOutput.currentPage());
        assertEquals(10, actualOutput.perPage());
        assertEquals(2L, actualOutput.total());
        assertEquals(List.of("Filmes", "Séries"), actualOutput.items().stream().map(CategoryOutput::name).toList());
    }

    @Test
    void givenValidQuery_whenCallExecute_thenMapEachCategoryToItsOutput() {
        final var category = aCategory("1", "Filmes");
        when(categoryGateway.findAll(EXPECTED_QUERY)).thenReturn(new Pagination<>(0, 10, 1L, List.of(category)));

        final var actualOutput = useCase.execute(EXPECTED_QUERY);

        final var actualItem = actualOutput.items().getFirst();
        assertEquals("1", actualItem.id());
        assertEquals("Filmes", actualItem.name());
        assertEquals("A categoria Filmes", actualItem.description());
        assertTrue(actualItem.active());
        assertEquals(EXPECTED_CREATED_AT, actualItem.createdAt());
        assertEquals(EXPECTED_UPDATED_AT, actualItem.updatedAt());
    }

    @Test
    void givenQueryWithoutResult_whenCallExecute_thenReturnAnEmptyPage() {
        when(categoryGateway.findAll(EXPECTED_QUERY)).thenReturn(new Pagination<>(0, 10, 0L, List.of()));

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
        when(categoryGateway.findAll(EXPECTED_QUERY)).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(EXPECTED_QUERY));

        assertSame(expectedException, actualException);
    }

    private static Category aCategory(final String id, final String name) {
        return Category.with(
                CategoryID.from(id),
                name,
                "A categoria %s".formatted(name),
                true,
                EXPECTED_CREATED_AT,
                EXPECTED_UPDATED_AT);
    }
}
