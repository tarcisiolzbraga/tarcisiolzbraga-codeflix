package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class GenreSearchRequestTest {

    @Test
    void givenNoParam_whenCallToSearchQuery_thenUseTheDocumentedDefaults() {
        final var request = new GenreSearchRequest(null, null, null, null, null);

        final var actualQuery = request.toSearchQuery();

        assertEquals(0, actualQuery.page());
        assertEquals(10, actualQuery.perPage());
        assertEquals("name", actualQuery.sort());
        assertEquals("asc", actualQuery.direction());
        assertNull(actualQuery.terms());
    }

    @Test
    void givenEveryParam_whenCallToSearchQuery_thenKeepThem() {
        final var request = new GenreSearchRequest("aca", 2, 25, "createdAt", "desc");

        final var actualQuery = request.toSearchQuery();

        assertEquals(2, actualQuery.page());
        assertEquals(25, actualQuery.perPage());
        assertEquals("createdAt", actualQuery.sort());
        assertEquals("desc", actualQuery.direction());
        assertEquals("aca", actualQuery.terms());
    }

    @Test
    void givenOnlySearchTerm_whenCallToSearchQuery_thenKeepTheOtherDefaults() {
        final var request = new GenreSearchRequest("aca", null, null, null, null);

        final var actualQuery = request.toSearchQuery();

        assertEquals("aca", actualQuery.terms());
        assertEquals(0, actualQuery.page());
        assertEquals("name", actualQuery.sort());
    }
}
