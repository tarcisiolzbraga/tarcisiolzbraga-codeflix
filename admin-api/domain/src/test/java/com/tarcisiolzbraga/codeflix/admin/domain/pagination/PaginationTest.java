package com.tarcisiolzbraga.codeflix.admin.domain.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaginationTest {

    @Test
    void givenItems_whenCallMap_thenKeepMetadataAndTransformItems() {
        final var pagination = new Pagination<>(1, 10, 42L, List.of("um", "dois"));

        final var actualPagination = pagination.map(String::length);

        assertEquals(1, actualPagination.currentPage());
        assertEquals(10, actualPagination.perPage());
        assertEquals(42L, actualPagination.total());
        assertEquals(List.of(2, 4), actualPagination.items());
    }

    @Test
    void givenMutableList_whenChangeItAfterCreation_thenPaginationIsNotAffected() {
        final var items = new ArrayList<>(List.of("um"));
        final var pagination = new Pagination<>(0, 10, 1L, items);

        items.add("dois");

        assertEquals(List.of("um"), pagination.items());
    }

    @Test
    void givenPagination_whenChangeItsItems_thenThrowUnsupportedOperationException() {
        final var pagination = new Pagination<>(0, 10, 1L, List.of("um"));

        final var actualItems = pagination.items();

        assertThrows(UnsupportedOperationException.class, () -> actualItems.add("dois"));
    }
}
