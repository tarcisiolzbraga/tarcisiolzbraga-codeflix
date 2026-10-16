package com.tarcisiolzbraga.codeflix.videos.infrastructure.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.domain.category.Category;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.persistence.CategoryDocument;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.persistence.CategoryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@IntegrationTest
class CategoryElasticsearchGatewayIT {

    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    @Autowired
    private CategoryElasticsearchGateway gateway;

    @Autowired
    private ElasticsearchOperations operations;

    // O repositório entra para provar que o inativo continua gravado: pelo gateway isso não é
    // observável, e não deve ser — nenhuma leitura pública devolve inativo.
    @Autowired
    private CategoryRepository repository;

    @Test
    void givenACategory_whenCallSave_thenStoreItAndReadItBackWhole() {
        final var category = aCategory("00000001-0000-0000-0000-000000000000", "Filmes", "A mais assistida", true);

        final var actualCategory = gateway.save(category);

        assertEquals(category, actualCategory);
        final var stored = gateway.findById(CategoryID.from("00000001-0000-0000-0000-000000000000")).orElseThrow();
        assertEquals("Filmes", stored.getName());
        assertEquals("A mais assistida", stored.getDescription());
        assertTrue(stored.isActive());
        assertEquals(CREATED_AT, stored.getCreatedAt());
        assertEquals(UPDATED_AT, stored.getUpdatedAt());
    }

    @Test
    void givenACategoryAlreadyStored_whenCallSaveAgain_thenReplaceIt() {
        gateway.save(aCategory("00000001-0000-0000-0000-000000000000", "Filmes", "A mais assistida", true));

        gateway.save(aCategory("00000001-0000-0000-0000-000000000000", "Filmes e séries", "texto novo", true));

        final var stored = gateway.findById(CategoryID.from("00000001-0000-0000-0000-000000000000")).orElseThrow();
        assertEquals("Filmes e séries", stored.getName());
        assertEquals("texto novo", stored.getDescription());
        assertEquals(1L, countListed());
    }

    @Test
    void givenAnUnknownId_whenCallFindById_thenReturnEmpty() {
        final var actualCategory = gateway.findById(CategoryID.from("00000023-0000-0000-0000-000000000000"));

        assertTrue(actualCategory.isEmpty());
    }

    @Test
    void givenAStoredCategory_whenCallDeleteById_thenRemoveIt() {
        gateway.save(aCategory("00000001-0000-0000-0000-000000000000", "Filmes", "A mais assistida", true));

        gateway.deleteById(CategoryID.from("00000001-0000-0000-0000-000000000000"));

        assertTrue(gateway.findById(CategoryID.from("00000001-0000-0000-0000-000000000000")).isEmpty());
    }

    @Test
    void givenAnUnknownId_whenCallDeleteById_thenDoNotComplain() {
        gateway.deleteById(CategoryID.from("00000023-0000-0000-0000-000000000000"));

        assertEquals(0L, countListed());
    }

    @Test
    void givenKnownIds_whenCallFindAllById_thenReturnOnlyThoseFound() {
        gateway.save(aCategory("00000001-0000-0000-0000-000000000000", "Filmes", "A mais assistida", true));
        gateway.save(aCategory("00000002-0000-0000-0000-000000000000", "Séries", "A segunda", true));

        final var actualCategories =
                gateway.findAllById(Set.of(CategoryID.from("00000001-0000-0000-0000-000000000000"), CategoryID.from("00000003-0000-0000-0000-000000000000")));

        assertEquals(1, actualCategories.size());
        assertEquals("00000001-0000-0000-0000-000000000000", actualCategories.getFirst().getId().getValue());
    }

    @Test
    void givenNoId_whenCallFindAllById_thenReturnEmpty() {
        final var actualCategories = gateway.findAllById(Set.of());

        assertTrue(actualCategories.isEmpty());
    }

    @Test
    void givenStoredCategories_whenCallFindAllWithoutTerms_thenReturnThePageSortedByName() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, null, "name", "asc"));

        assertEquals(0, actualPage.currentPage());
        assertEquals(10, actualPage.perPage());
        assertEquals(3L, actualPage.total());
        assertEquals(List.of("Documentários", "Filmes", "Séries"), namesOf(actualPage.items()));
    }

    @Test
    void givenADescendingDirection_whenCallFindAll_thenReverseTheOrder() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, null, "name", "desc"));

        assertEquals(List.of("Séries", "Filmes", "Documentários"), namesOf(actualPage.items()));
    }

    @Test
    void givenASecondPage_whenCallFindAll_thenReturnOnlyItsItemsAndTheFullTotal() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(1, 2, null, "name", "asc"));

        assertEquals(1, actualPage.currentPage());
        assertEquals(3L, actualPage.total());
        assertEquals(List.of("Séries"), namesOf(actualPage.items()));
    }

    @Test
    void givenTermsMatchingTheName_whenCallFindAll_thenReturnOnlyWhatMatches() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, "Documentários", "name", "asc"));

        assertEquals(1L, actualPage.total());
        assertEquals(List.of("Documentários"), namesOf(actualPage.items()));
    }

    @Test
    void givenTermsMatchingOnlyTheDescription_whenCallFindAll_thenStillFindIt() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, "segunda", "name", "asc"));

        assertEquals(1L, actualPage.total());
        assertEquals(List.of("Séries"), namesOf(actualPage.items()));
    }

    @Test
    void givenTermsThatMatchNothing_whenCallFindAll_thenReturnAnEmptyPage() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, "xpto", "name", "asc"));

        assertEquals(0L, actualPage.total());
        assertTrue(actualPage.items().isEmpty());
    }

    @Test
    void givenAnInactiveCategory_whenCallFindAll_thenHideIt() {
        gateway.save(aCategory("00000001-0000-0000-0000-000000000000", "Filmes", "A mais assistida", true));
        gateway.save(aCategory("00000002-0000-0000-0000-000000000000", "Séries", "Desativada no admin", false));
        refresh();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, null, "name", "asc"));

        assertEquals(1L, actualPage.total());
        assertEquals(List.of("Filmes"), namesOf(actualPage.items()));
    }

    @Test
    void givenAnInactiveCategory_whenCallFindById_thenReturnEmptyAsIfItWereNotThere() {
        gateway.save(aCategory("00000001-0000-0000-0000-000000000000", "Filmes", "Desativada no admin", false));

        final var actualCategory = gateway.findById(CategoryID.from("00000001-0000-0000-0000-000000000000"));

        assertTrue(actualCategory.isEmpty());
    }

    // O filtro é na leitura, não na gravação: reativar no admin faz reaparecer na hora, sem recarga.
    @Test
    void givenAnInactiveCategory_whenCallSave_thenStillStoreIt() {
        gateway.save(aCategory("00000001-0000-0000-0000-000000000000", "Filmes", "Desativada no admin", false));

        final var stored = repository.findById(UUID.fromString("00000001-0000-0000-0000-000000000000"));

        assertTrue(stored.isPresent());
        assertFalse(stored.orElseThrow().isActive());
    }

    // É daqui que sai a resolução de relação: um vídeo ativo com categorias inativas vem sem elas.
    @Test
    void givenAMixOfActiveAndInactiveIds_whenCallFindAllById_thenLeaveTheInactiveOnesOut() {
        gateway.save(aCategory("00000001-0000-0000-0000-000000000000", "Filmes", "A mais assistida", true));
        gateway.save(aCategory("00000002-0000-0000-0000-000000000000", "Séries", "Desativada no admin", false));

        final var actualCategories =
                gateway.findAllById(Set.of(CategoryID.from("00000001-0000-0000-0000-000000000000"), CategoryID.from("00000002-0000-0000-0000-000000000000")));

        assertEquals(1, actualCategories.size());
        assertEquals("00000001-0000-0000-0000-000000000000", actualCategories.getFirst().getId().getValue());
    }

    @Test
    void givenAnInactiveCategoryWhoseNameMatches_whenSearchByTerms_thenStillHideIt() {
        gateway.save(aCategory("00000001-0000-0000-0000-000000000000", "Filmes", "Desativada no admin", false));
        refresh();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, "Filmes", "name", "asc"));

        assertEquals(0L, actualPage.total());
    }

    // Termo com espaço é o caso que o contains sozinho não atende: o Spring Data recusa construir
    // um wildcard com espaço dentro, e a listagem inteira falhava. Cada palavra entra separada, e
    // todas são exigidas.
    @Test
    void givenTermsWithSeveralWords_whenCallFindAll_thenRequireAllOfThem() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, "mais assistida", "name", "asc"));

        assertEquals(2L, actualPage.total());
        assertEquals(List.of("Filmes", "Séries"), namesOf(actualPage.items()));
    }

    @Test
    void givenEachWordInADifferentField_whenCallFindAll_thenStillFindIt() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, "Séries segunda", "name", "asc"));

        assertEquals(1L, actualPage.total());
        assertEquals(List.of("Séries"), namesOf(actualPage.items()));
    }

    @Test
    void givenTermsWhereOneWordMatchesNothing_whenCallFindAll_thenReturnAnEmptyPage() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, "Filmes xpto", "name", "asc"));

        assertEquals(0L, actualPage.total());
    }

    private void seed() {
        gateway.save(aCategory("00000001-0000-0000-0000-000000000000", "Filmes", "A mais assistida", true));
        gateway.save(aCategory("00000002-0000-0000-0000-000000000000", "Séries", "A segunda mais assistida", true));
        gateway.save(aCategory("00000003-0000-0000-0000-000000000000", "Documentários", "Catálogo de documentários", true));
        refresh();
    }

    // O Elasticsearch é quase em tempo real: a busca só vê o documento depois do refresh do índice.
    // Ler por id não precisa disso, porque o GET por id é servido direto do que já foi gravado.
    private void refresh() {
        operations.indexOps(CategoryDocument.class).refresh();
    }

    // Conta o que a listagem mostra, que não é o mesmo que está gravado: a listagem esconde inativos.
    private long countListed() {
        refresh();
        return gateway.findAll(new SearchQuery(0, 100, null, "name", "asc")).total();
    }

    private static List<String> namesOf(final List<Category> categories) {
        return categories.stream().map(Category::getName).toList();
    }

    private static Category aCategory(
            final String id, final String name, final String description, final boolean active) {
        return Category.with(CategoryID.from(id), name, description, active, CREATED_AT, UPDATED_AT);
    }
}
