package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import org.junit.jupiter.api.Test;

class CastMemberSearchRequestTest {

    @Test
    void givenNoParam_whenCallToSearchQuery_thenUseTheDocumentedDefaults() {
        final var request = new CastMemberSearchRequest(null, null, null, null, null);

        final var actualQuery = request.toSearchQuery();

        assertEquals(new SearchQuery(0, 10, null, "name", "asc"), actualQuery);
    }

    @Test
    void givenEveryParam_whenCallToSearchQuery_thenKeepThem() {
        final var request = new CastMemberSearchRequest("vin", 2, 25, "createdAt", "desc");

        final var actualQuery = request.toSearchQuery();

        assertEquals(new SearchQuery(2, 25, "vin", "createdAt", "desc"), actualQuery);
    }

    @Test
    void givenOnlySearchTerm_whenCallToSearchQuery_thenKeepTheOtherDefaults() {
        final var request = new CastMemberSearchRequest("vin", null, null, null, null);

        final var actualQuery = request.toSearchQuery();

        assertEquals("vin", actualQuery.terms());
        assertEquals(0, actualQuery.page());
        assertNull(new CastMemberSearchRequest(null, null, null, null, null).search());
    }
}
