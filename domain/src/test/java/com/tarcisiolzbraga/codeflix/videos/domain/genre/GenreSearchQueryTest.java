package com.tarcisiolzbraga.codeflix.videos.domain.genre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GenreSearchQueryTest {

    @Test
    void givenCategories_whenInstantiate_thenHoldThem() {
        final var categories = Set.of(CategoryID.from("1"), CategoryID.from("2"));

        final var actualQuery = new GenreSearchQuery(0, 10, "aç", "name", "asc", categories);

        assertEquals(categories, actualQuery.categories());
    }

    @Test
    void givenNullCategories_whenInstantiate_thenTreatThemAsNoneInsteadOfThrowing() {
        final var actualQuery = new GenreSearchQuery(0, 10, null, "name", "asc", null);

        assertTrue(actualQuery.categories().isEmpty());
    }

    @Test
    void givenAMutableSet_whenInstantiate_thenCopyItSoLaterChangesDoNotLeakIn() {
        final var categories = new HashSet<CategoryID>();
        categories.add(CategoryID.from("1"));
        final var actualQuery = new GenreSearchQuery(0, 10, null, "name", "asc", categories);

        categories.add(CategoryID.from("2"));

        assertEquals(1, actualQuery.categories().size());
    }

    @Test
    void givenAQuery_whenChangeTheReturnedCategories_thenRefuseTheChange() {
        final var actualQuery = new GenreSearchQuery(0, 10, null, "name", "asc", Set.of(CategoryID.from("1")));

        final var actualCategories = actualQuery.categories();

        assertThrows(UnsupportedOperationException.class, () -> actualCategories.add(CategoryID.from("2")));
    }

    // O teto vale também para as queries com filtro próprio: a regra é do tamanho da página, não da
    // listagem que a pede.
    @Test
    void givenAPerPageAboveTheCeiling_whenInstantiate_thenRefuseIt() {
        final var actualException = assertThrows(
                DomainException.class, () -> new GenreSearchQuery(0, 101, "", "name", "asc", Set.of()));

        assertEquals("'perPage' should be between 1 and 100", actualException.getMessage());
    }
}
