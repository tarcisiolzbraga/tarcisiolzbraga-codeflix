package com.tarcisiolzbraga.codeflix.videos.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VideoSearchQueryTest {

    @Test
    void givenEveryFilter_whenInstantiate_thenHoldThem() {
        final var actualQuery = new VideoSearchQuery(
                0,
                10,
                "duna",
                "title",
                "asc",
                Rating.AGE_14,
                2026,
                Set.of(CategoryID.from("c1")),
                Set.of(GenreID.from("g1")),
                Set.of(CastMemberID.from("m1")));

        assertEquals(Rating.AGE_14, actualQuery.rating());
        assertEquals(2026, actualQuery.launchedAt());
        assertEquals(Set.of(CategoryID.from("c1")), actualQuery.categories());
    }

    // Filtro ausente não é erro: a query sem relação alguma é a listagem simples.
    @Test
    void givenNullSets_whenInstantiate_thenTreatThemAsNoFilterInsteadOfThrowing() {
        final var actualQuery = new VideoSearchQuery(0, 10, null, "title", "asc", null, null, null, null, null);

        assertTrue(actualQuery.categories().isEmpty());
        assertTrue(actualQuery.genres().isEmpty());
        assertTrue(actualQuery.castMembers().isEmpty());
    }

    @Test
    void givenAMutableSet_whenInstantiate_thenCopyItSoLaterChangesDoNotLeakIn() {
        final var genres = new HashSet<GenreID>();
        genres.add(GenreID.from("g1"));
        final var actualQuery = new VideoSearchQuery(0, 10, null, "title", "asc", null, null, null, genres, null);

        genres.add(GenreID.from("g2"));

        assertEquals(1, actualQuery.genres().size());
    }

    @Test
    void givenAQuery_whenChangeTheReturnedSets_thenRefuseTheChange() {
        final var actualQuery = new VideoSearchQuery(
                0, 10, null, "title", "asc", null, null, Set.of(CategoryID.from("c1")), null, null);

        final var actualCategories = actualQuery.categories();

        assertThrows(UnsupportedOperationException.class, () -> actualCategories.add(CategoryID.from("c2")));
    }

    // O teto vale também para as queries com filtro próprio: a regra é do tamanho da página, não da
    // listagem que a pede.
    @Test
    void givenAPerPageAboveTheCeiling_whenInstantiate_thenRefuseIt() {
        final var actualException = assertThrows(DomainException.class, () -> new VideoSearchQuery(
                0, 101, "", "title", "asc", null, null, Set.of(), Set.of(), Set.of()));

        assertEquals("'perPage' should be between 1 and 100", actualException.getMessage());
    }
}
