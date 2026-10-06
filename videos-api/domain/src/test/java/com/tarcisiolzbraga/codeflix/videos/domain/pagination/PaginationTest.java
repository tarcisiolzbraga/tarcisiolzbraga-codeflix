package com.tarcisiolzbraga.codeflix.videos.domain.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaginationTest {

    private static final int EXPECTED_PAGE = 2;
    private static final int EXPECTED_PER_PAGE = 10;
    private static final long EXPECTED_TOTAL = 37;

    @Test
    void givenAPagination_whenCallMap_thenTransformItemsAndKeepTheMetadata() {
        final var pagination = new Pagination<>(EXPECTED_PAGE, EXPECTED_PER_PAGE, EXPECTED_TOTAL, List.of(1, 2, 3));

        final var actualPagination = pagination.map(String::valueOf);

        assertEquals(EXPECTED_PAGE, actualPagination.currentPage());
        assertEquals(EXPECTED_PER_PAGE, actualPagination.perPage());
        assertEquals(EXPECTED_TOTAL, actualPagination.total());
        assertEquals(List.of("1", "2", "3"), actualPagination.items());
    }

    @Test
    void givenAMutableList_whenInstantiate_thenCopyItSoLaterChangesDoNotLeakIn() {
        final var items = new ArrayList<>(List.of("filmes"));
        final var pagination = new Pagination<>(EXPECTED_PAGE, EXPECTED_PER_PAGE, EXPECTED_TOTAL, items);

        items.add("séries");

        assertEquals(List.of("filmes"), pagination.items());
    }

    @Test
    void givenAPagination_whenChangeTheReturnedItems_thenRefuseTheChange() {
        final var pagination = new Pagination<>(EXPECTED_PAGE, EXPECTED_PER_PAGE, EXPECTED_TOTAL, List.of("filmes"));

        final var actualItems = pagination.items();

        assertThrows(UnsupportedOperationException.class, () -> actualItems.add("séries"));
    }

    @Test
    void givenAnEmptyPage_whenCallMap_thenReturnAnEmptyPageWithTheSameMetadata() {
        final var pagination = new Pagination<Integer>(EXPECTED_PAGE, EXPECTED_PER_PAGE, EXPECTED_TOTAL, List.of());

        final var actualPagination = pagination.map(String::valueOf);

        assertEquals(EXPECTED_TOTAL, actualPagination.total());
        assertEquals(List.of(), actualPagination.items());
    }
}
