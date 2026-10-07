package com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.persistence.CastMemberDocument;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.persistence.CastMemberRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@IntegrationTest
class CastMemberElasticsearchGatewayIT {

    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    @Autowired
    private CastMemberElasticsearchGateway gateway;

    @Autowired
    private ElasticsearchOperations operations;

    // O repositório entra para provar que o inativo continua gravado: pelo gateway isso não é
    // observável, e não deve ser.
    @Autowired
    private CastMemberRepository repository;

    @Test
    void givenAMember_whenCallSave_thenStoreItAndReadItBackWhole() {
        final var member = aMember("00000001-0000-0000-0000-000000000000", "Denis Villeneuve", CastMemberType.DIRECTOR, true);

        final var actualMember = gateway.save(member);

        assertEquals(member, actualMember);
        final var stored = gateway.findById(CastMemberID.from("00000001-0000-0000-0000-000000000000")).orElseThrow();
        assertEquals("Denis Villeneuve", stored.getName());
        assertEquals(CastMemberType.DIRECTOR, stored.getType());
        assertTrue(stored.isActive());
        assertEquals(CREATED_AT, stored.getCreatedAt());
        assertEquals(UPDATED_AT, stored.getUpdatedAt());
    }

    // O filtro é na leitura, não na gravação: reativar no admin faz reaparecer na hora, sem recarga.
    @Test
    void givenAnInactiveMember_whenCallSave_thenStillStoreItButHideItFromEveryRead() {
        gateway.save(aMember("00000001-0000-0000-0000-000000000000", "Denis Villeneuve", CastMemberType.DIRECTOR, false));

        final var stored = repository.findById(UUID.fromString("00000001-0000-0000-0000-000000000000"));

        assertTrue(stored.isPresent());
        assertFalse(stored.orElseThrow().isActive());
        assertTrue(gateway.findById(CastMemberID.from("00000001-0000-0000-0000-000000000000")).isEmpty());
    }

    @Test
    void givenAMemberAlreadyStored_whenCallSaveAgain_thenReplaceIt() {
        gateway.save(aMember("00000001-0000-0000-0000-000000000000", "Denis Villeneuve", CastMemberType.DIRECTOR, true));

        gateway.save(aMember("00000001-0000-0000-0000-000000000000", "Denis Villeneuve", CastMemberType.ACTOR, true));

        final var stored = gateway.findById(CastMemberID.from("00000001-0000-0000-0000-000000000000")).orElseThrow();
        assertEquals(CastMemberType.ACTOR, stored.getType());
        assertEquals(1L, countListed());
    }

    @Test
    void givenAnUnknownId_whenCallFindById_thenReturnEmpty() {
        final var actualMember = gateway.findById(CastMemberID.from("00000023-0000-0000-0000-000000000000"));

        assertTrue(actualMember.isEmpty());
    }

    @Test
    void givenAStoredMember_whenCallDeleteById_thenRemoveIt() {
        gateway.save(aMember("00000001-0000-0000-0000-000000000000", "Denis Villeneuve", CastMemberType.DIRECTOR, true));

        gateway.deleteById(CastMemberID.from("00000001-0000-0000-0000-000000000000"));

        assertTrue(gateway.findById(CastMemberID.from("00000001-0000-0000-0000-000000000000")).isEmpty());
    }

    @Test
    void givenAnUnknownId_whenCallDeleteById_thenDoNotComplain() {
        gateway.deleteById(CastMemberID.from("00000023-0000-0000-0000-000000000000"));

        assertEquals(0L, countListed());
    }

    @Test
    void givenKnownIds_whenCallFindAllById_thenReturnOnlyThoseFound() {
        gateway.save(aMember("00000001-0000-0000-0000-000000000000", "Denis Villeneuve", CastMemberType.DIRECTOR, true));
        gateway.save(aMember("00000002-0000-0000-0000-000000000000", "Timothée Chalamet", CastMemberType.ACTOR, true));

        final var actualMembers = gateway.findAllById(Set.of(CastMemberID.from("00000001-0000-0000-0000-000000000000"), CastMemberID.from("00000003-0000-0000-0000-000000000000")));

        assertEquals(1, actualMembers.size());
        assertEquals("00000001-0000-0000-0000-000000000000", actualMembers.getFirst().getId().getValue());
    }

    @Test
    void givenNoId_whenCallFindAllById_thenReturnEmpty() {
        final var actualMembers = gateway.findAllById(Set.of());

        assertTrue(actualMembers.isEmpty());
    }

    @Test
    void givenStoredMembers_whenCallFindAllWithoutTerms_thenReturnThePageSortedByName() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, null, "name", "asc"));

        assertEquals(3L, actualPage.total());
        assertEquals(List.of("Denis Villeneuve", "Rebecca Ferguson", "Timothée Chalamet"), namesOf(actualPage));
    }

    @Test
    void givenADescendingDirection_whenCallFindAll_thenReverseTheOrder() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, null, "name", "desc"));

        assertEquals(List.of("Timothée Chalamet", "Rebecca Ferguson", "Denis Villeneuve"), namesOf(actualPage));
    }

    @Test
    void givenASecondPage_whenCallFindAll_thenReturnOnlyItsItemsAndTheFullTotal() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(1, 2, null, "name", "asc"));

        assertEquals(1, actualPage.currentPage());
        assertEquals(3L, actualPage.total());
        assertEquals(List.of("Timothée Chalamet"), namesOf(actualPage));
    }

    @Test
    void givenTermsMatchingTheName_whenCallFindAll_thenReturnOnlyWhatMatches() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, "Rebecca", "name", "asc"));

        assertEquals(1L, actualPage.total());
        assertEquals(List.of("Rebecca Ferguson"), namesOf(actualPage));
    }

    @Test
    void givenTermsThatMatchNothing_whenCallFindAll_thenReturnAnEmptyPage() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, "xpto", "name", "asc"));

        assertEquals(0L, actualPage.total());
        assertTrue(actualPage.items().isEmpty());
    }

    @Test
    void givenAnInactiveMember_whenCallFindAll_thenHideIt() {
        gateway.save(aMember("00000001-0000-0000-0000-000000000000", "Denis Villeneuve", CastMemberType.DIRECTOR, true));
        gateway.save(aMember("00000002-0000-0000-0000-000000000000", "Rebecca Ferguson", CastMemberType.ACTOR, false));
        refresh();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, null, "name", "asc"));

        assertEquals(1L, actualPage.total());
        assertEquals(List.of("Denis Villeneuve"), namesOf(actualPage));
    }

    // É daqui que sai a resolução de relação: um vídeo ativo com membros inativos vem sem eles.
    @Test
    void givenAMixOfActiveAndInactiveIds_whenCallFindAllById_thenLeaveTheInactiveOnesOut() {
        gateway.save(aMember("00000001-0000-0000-0000-000000000000", "Denis Villeneuve", CastMemberType.DIRECTOR, true));
        gateway.save(aMember("00000002-0000-0000-0000-000000000000", "Rebecca Ferguson", CastMemberType.ACTOR, false));

        final var actualMembers = gateway.findAllById(Set.of(CastMemberID.from("00000001-0000-0000-0000-000000000000"), CastMemberID.from("00000002-0000-0000-0000-000000000000")));

        assertEquals(1, actualMembers.size());
        assertEquals("00000001-0000-0000-0000-000000000000", actualMembers.getFirst().getId().getValue());
    }

    @Test
    void givenAnInactiveMemberWhoseNameMatches_whenSearchByTerms_thenStillHideIt() {
        gateway.save(aMember("00000001-0000-0000-0000-000000000000", "Denis Villeneuve", CastMemberType.DIRECTOR, false));
        refresh();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, "Denis", "name", "asc"));

        assertEquals(0L, actualPage.total());
    }

    // Nome completo é o termo mais natural aqui, e é justamente o que tem espaço.
    @Test
    void givenTermsWithSeveralWords_whenCallFindAll_thenRequireAllOfThem() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, "Denis Villeneuve", "name", "asc"));

        assertEquals(List.of("Denis Villeneuve"), namesOf(actualPage));
    }

    @Test
    void givenTermsWhereOneWordMatchesNothing_whenCallFindAll_thenReturnAnEmptyPage() {
        seed();

        final var actualPage = gateway.findAll(new SearchQuery(0, 10, "Denis xpto", "name", "asc"));

        assertEquals(0L, actualPage.total());
    }

    private void seed() {
        gateway.save(aMember("00000001-0000-0000-0000-000000000000", "Denis Villeneuve", CastMemberType.DIRECTOR, true));
        gateway.save(aMember("00000002-0000-0000-0000-000000000000", "Timothée Chalamet", CastMemberType.ACTOR, true));
        gateway.save(aMember("00000003-0000-0000-0000-000000000000", "Rebecca Ferguson", CastMemberType.ACTOR, true));
        refresh();
    }

    // O Elasticsearch é quase em tempo real: a busca só vê o documento depois do refresh do índice.
    // Ler por id não precisa, porque o GET por id é servido direto do que já foi gravado.
    private void refresh() {
        operations.indexOps(CastMemberDocument.class).refresh();
    }

    // Conta o que a listagem mostra, que não é o mesmo que está gravado: a listagem esconde inativos.
    private long countListed() {
        refresh();
        return gateway.findAll(new SearchQuery(0, 100, null, "name", "asc")).total();
    }

    private static List<String> namesOf(final Pagination<CastMember> page) {
        return page.items().stream().map(CastMember::getName).toList();
    }

    private static CastMember aMember(
            final String id, final String name, final CastMemberType type, final boolean active) {
        return CastMember.with(CastMemberID.from(id), name, type, active, CREATED_AT, UPDATED_AT);
    }
}
