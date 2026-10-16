package com.tarcisiolzbraga.codeflix.videos.domain.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import org.junit.jupiter.api.Test;

// O teto da página é a regra que fecha a de nunca devolver coleção sem limite: paginar sem limitar o
// tamanho é devolver o acervo inteiro com um nome melhor, bastando o cliente pedir perPage grande.
class SearchQueryTest {

    private static final String OUT_OF_RANGE = "'perPage' should be between 1 and 100";

    @Test
    void givenAPerPageWithinTheCeiling_whenCreateTheQuery_thenKeepIt() {
        final var actualQuery = new SearchQuery(0, 10, "", "name", "asc");

        assertEquals(10, actualQuery.perPage());
    }

    @Test
    void givenAPerPageExactlyAtTheCeiling_whenCreateTheQuery_thenKeepIt() {
        final var actualQuery = new SearchQuery(0, PageSize.MAX, "", "name", "asc");

        assertEquals(100, actualQuery.perPage());
    }

    @Test
    void givenAPerPageAboveTheCeiling_whenCreateTheQuery_thenRefuseIt() {
        final var actualException =
                assertThrows(DomainException.class, () -> new SearchQuery(0, 101, "", "name", "asc"));

        assertEquals(OUT_OF_RANGE, actualException.getMessage());
    }

    @Test
    void givenAPerPageBelowOne_whenCreateTheQuery_thenRefuseIt() {
        final var actualException =
                assertThrows(DomainException.class, () -> new SearchQuery(0, 0, "", "name", "asc"));

        assertEquals(OUT_OF_RANGE, actualException.getMessage());
    }
}
