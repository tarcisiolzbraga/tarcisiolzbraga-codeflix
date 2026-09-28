package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VideoSearchRequestTest {

    @Test
    void givenNoParam_whenCallToSearchQuery_thenUseTheDocumentedDefaults() {
        final var request = new VideoSearchRequest(null, null, null, null, null, null, null, null);

        final var actualQuery = request.toSearchQuery();

        assertEquals(new SearchQuery(0, 10, null, "title", "asc"), actualQuery.page());
        assertTrue(actualQuery.categories().isEmpty());
        assertTrue(actualQuery.genres().isEmpty());
        assertTrue(actualQuery.castMembers().isEmpty());
    }

    @Test
    void givenEveryPageParam_whenCallToSearchQuery_thenKeepThem() {
        final var request = new VideoSearchRequest("duna", 2, 25, "createdAt", "desc", null, null, null);

        final var actualQuery = request.toSearchQuery();

        assertEquals(new SearchQuery(2, 25, "duna", "createdAt", "desc"), actualQuery.page());
    }

    @Test
    void givenReferenceFilters_whenCallToSearchQuery_thenConvertThemToTypedIds() {
        final var categoryId = CategoryID.unique().getValue();
        final var request = new VideoSearchRequest(
                null, null, null, null, null, Set.of(categoryId), Set.of("g1"), Set.of("m1"));

        final var actualQuery = request.toSearchQuery();

        assertEquals(Set.of(CategoryID.from(categoryId)), actualQuery.categories());
        assertEquals(1, actualQuery.genres().size());
        assertEquals(1, actualQuery.castMembers().size());
    }
}
