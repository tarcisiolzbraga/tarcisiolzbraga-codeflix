package com.tarcisiolzbraga.codeflix.videos.infrastructure.genre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreSearchQuery;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.persistence.GenreDocument;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.persistence.GenreRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@IntegrationTest
class GenreElasticsearchGatewayIT {

    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");
    private static final CategoryID FILMES = CategoryID.from("00000005-0000-0000-0000-000000000000");
    private static final CategoryID SERIES = CategoryID.from("00000008-0000-0000-0000-000000000000");

    @Autowired
    private GenreElasticsearchGateway gateway;

    @Autowired
    private ElasticsearchOperations operations;

    @Autowired
    private GenreRepository repository;

    @Test
    void givenAGenre_whenCallSave_thenStoreItAndReadItBackWithItsCategories() {
        final var genre = aGenre("00000001-0000-0000-0000-000000000000", "Ação", true, Set.of(FILMES, SERIES));

        final var actualGenre = gateway.save(genre);

        assertEquals(genre, actualGenre);
        final var stored = gateway.findById(GenreID.from("00000001-0000-0000-0000-000000000000")).orElseThrow();
        assertEquals("Ação", stored.getName());
        assertEquals(Set.of(FILMES, SERIES), stored.getCategories());
        assertTrue(stored.isActive());
        assertEquals(CREATED_AT, stored.getCreatedAt());
    }

    @Test
    void givenAGenreWithoutCategories_whenCallSave_thenReadItBackWithAnEmptySet() {
        gateway.save(aGenre("00000001-0000-0000-0000-000000000000", "Ação", true, Set.of()));

        final var stored = gateway.findById(GenreID.from("00000001-0000-0000-0000-000000000000")).orElseThrow();

        assertTrue(stored.getCategories().isEmpty());
    }

    @Test
    void givenAGenreAlreadyStored_whenCallSaveAgain_thenReplaceItIncludingTheCategories() {
        gateway.save(aGenre("00000001-0000-0000-0000-000000000000", "Ação", true, Set.of(FILMES, SERIES)));

        gateway.save(aGenre("00000001-0000-0000-0000-000000000000", "Ação e aventura", true, Set.of(FILMES)));

        final var stored = gateway.findById(GenreID.from("00000001-0000-0000-0000-000000000000")).orElseThrow();
        assertEquals("Ação e aventura", stored.getName());
        assertEquals(Set.of(FILMES), stored.getCategories());
    }

    @Test
    void givenAnUnknownId_whenCallFindById_thenReturnEmpty() {
        assertTrue(gateway.findById(GenreID.from("00000023-0000-0000-0000-000000000000")).isEmpty());
    }

    @Test
    void givenAnInactiveGenre_whenCallFindById_thenReturnEmptyButKeepItStored() {
        gateway.save(aGenre("00000001-0000-0000-0000-000000000000", "Ação", false, Set.of(FILMES)));

        final var actualGenre = gateway.findById(GenreID.from("00000001-0000-0000-0000-000000000000"));

        assertTrue(actualGenre.isEmpty());
        assertTrue(repository.findById("00000001-0000-0000-0000-000000000000").isPresent());
        assertFalse(repository.findById("00000001-0000-0000-0000-000000000000").orElseThrow().isActive());
    }

    @Test
    void givenAStoredGenre_whenCallDeleteById_thenRemoveIt() {
        gateway.save(aGenre("00000001-0000-0000-0000-000000000000", "Ação", true, Set.of(FILMES)));

        gateway.deleteById(GenreID.from("00000001-0000-0000-0000-000000000000"));

        assertTrue(gateway.findById(GenreID.from("00000001-0000-0000-0000-000000000000")).isEmpty());
        assertTrue(repository.findById("00000001-0000-0000-0000-000000000000").isEmpty());
    }

    @Test
    void givenAMixOfActiveAndInactiveIds_whenCallFindAllById_thenLeaveTheInactiveOnesOut() {
        gateway.save(aGenre("00000001-0000-0000-0000-000000000000", "Ação", true, Set.of(FILMES)));
        gateway.save(aGenre("00000002-0000-0000-0000-000000000000", "Comédia", false, Set.of(FILMES)));

        final var actualGenres = gateway.findAllById(Set.of(GenreID.from("00000001-0000-0000-0000-000000000000"), GenreID.from("00000002-0000-0000-0000-000000000000")));

        assertEquals(1, actualGenres.size());
        assertEquals("00000001-0000-0000-0000-000000000000", actualGenres.getFirst().getId().getValue());
    }

    @Test
    void givenNoId_whenCallFindAllById_thenReturnEmpty() {
        assertTrue(gateway.findAllById(Set.of()).isEmpty());
    }

    @Test
    void givenStoredGenres_whenCallFindAllWithoutFilters_thenReturnThePageSortedByName() {
        seed();

        final var actualPage = gateway.findAll(aQuery(null, Set.of(), 0, 10));

        assertEquals(3L, actualPage.total());
        assertEquals(List.of("Ação", "Comédia", "Drama"), namesOf(actualPage));
    }

    @Test
    void givenAnInactiveGenre_whenCallFindAll_thenHideIt() {
        gateway.save(aGenre("00000001-0000-0000-0000-000000000000", "Ação", true, Set.of(FILMES)));
        gateway.save(aGenre("00000002-0000-0000-0000-000000000000", "Comédia", false, Set.of(FILMES)));
        refresh();

        final var actualPage = gateway.findAll(aQuery(null, Set.of(), 0, 10));

        assertEquals(1L, actualPage.total());
        assertEquals(List.of("Ação"), namesOf(actualPage));
    }

    @Test
    void givenTermsMatchingTheName_whenCallFindAll_thenReturnOnlyWhatMatches() {
        seed();

        final var actualPage = gateway.findAll(aQuery("Comédia", Set.of(), 0, 10));

        assertEquals(1L, actualPage.total());
        assertEquals(List.of("Comédia"), namesOf(actualPage));
    }

    // O filtro que só o gênero tem: trazer os gêneros de uma categoria.
    @Test
    void givenACategory_whenCallFindAll_thenReturnOnlyTheGenresLinkedToIt() {
        seed();

        final var actualPage = gateway.findAll(aQuery(null, Set.of(SERIES), 0, 10));

        assertEquals(1L, actualPage.total());
        assertEquals(List.of("Drama"), namesOf(actualPage));
    }

    @Test
    void givenSeveralCategories_whenCallFindAll_thenReturnTheGenresLinkedToAnyOfThem() {
        seed();

        final var actualPage = gateway.findAll(aQuery(null, Set.of(FILMES, SERIES), 0, 10));

        assertEquals(3L, actualPage.total());
    }

    @Test
    void givenACategoryAndTerms_whenCallFindAll_thenApplyBothFilters() {
        seed();

        final var actualPage = gateway.findAll(aQuery("Ação", Set.of(FILMES), 0, 10));

        assertEquals(1L, actualPage.total());
        assertEquals(List.of("Ação"), namesOf(actualPage));
    }

    @Test
    void givenACategoryNoGenreIsLinkedTo_whenCallFindAll_thenReturnAnEmptyPage() {
        seed();

        final var actualPage = gateway.findAll(aQuery(null, Set.of(CategoryID.from("00000007-0000-0000-0000-000000000000")), 0, 10));

        assertEquals(0L, actualPage.total());
        assertTrue(actualPage.items().isEmpty());
    }

    @Test
    void givenAnInactiveGenreLinkedToTheCategory_whenFilterByIt_thenStillHideIt() {
        gateway.save(aGenre("00000001-0000-0000-0000-000000000000", "Ação", false, Set.of(FILMES)));
        refresh();

        final var actualPage = gateway.findAll(aQuery(null, Set.of(FILMES), 0, 10));

        assertEquals(0L, actualPage.total());
    }

    @Test
    void givenASecondPage_whenCallFindAll_thenReturnOnlyItsItemsAndTheFullTotal() {
        seed();

        final var actualPage = gateway.findAll(aQuery(null, Set.of(), 1, 2));

        assertEquals(1, actualPage.currentPage());
        assertEquals(3L, actualPage.total());
        assertEquals(List.of("Drama"), namesOf(actualPage));
    }

    @Test
    void givenADescendingDirection_whenCallFindAll_thenReverseTheOrder() {
        seed();

        final var actualPage = gateway.findAll(
                new GenreSearchQuery(0, 10, null, "name", "desc", Set.of()));

        assertEquals(List.of("Drama", "Comédia", "Ação"), namesOf(actualPage));
    }

    @Test
    void givenTermsWithSeveralWords_whenCallFindAll_thenRequireAllOfThem() {
        seed();
        gateway.save(aGenre("00000003-5000-0000-0000-000000000000", "Ficção científica", true, Set.of(FILMES)));
        refresh();

        final var actualPage = gateway.findAll(aQuery("Ficção científica", Set.of(), 0, 10));

        assertEquals(List.of("Ficção científica"), namesOf(actualPage));
    }

    @Test
    void givenTermsWhereOneWordMatchesNothing_whenCallFindAll_thenReturnAnEmptyPage() {
        seed();

        final var actualPage = gateway.findAll(aQuery("Ação xpto", Set.of(), 0, 10));

        assertEquals(0L, actualPage.total());
    }

    private void seed() {
        gateway.save(aGenre("00000001-0000-0000-0000-000000000000", "Ação", true, Set.of(FILMES)));
        gateway.save(aGenre("00000002-0000-0000-0000-000000000000", "Comédia", true, Set.of(FILMES)));
        gateway.save(aGenre("00000003-0000-0000-0000-000000000000", "Drama", true, Set.of(FILMES, SERIES)));
        refresh();
    }

    // O Elasticsearch é quase em tempo real: a busca só vê o documento depois do refresh do índice.
    private void refresh() {
        operations.indexOps(GenreDocument.class).refresh();
    }

    private static GenreSearchQuery aQuery(
            final String terms, final Set<CategoryID> categories, final int page, final int perPage) {
        return new GenreSearchQuery(page, perPage, terms, "name", "asc", categories);
    }

    private static List<String> namesOf(final Pagination<Genre> page) {
        return page.items().stream().map(Genre::getName).toList();
    }

    private static Genre aGenre(
            final String id, final String name, final boolean active, final Set<CategoryID> categories) {
        return Genre.with(GenreID.from(id), name, active, categories, CREATED_AT, UPDATED_AT);
    }
}
