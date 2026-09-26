package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.persistence.CastMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class CastMemberMySQLGatewayIT {

    private static final String VIN_DIESEL = "Vin Diesel";
    private static final String SPIELBERG = "Steven Spielberg";
    private static final String NAME = "name";
    private static final String ASC = "asc";

    @Autowired
    private CastMemberMySQLGateway gateway;

    @Autowired
    private CastMemberRepository castMemberRepository;

    @Test
    void givenCastMember_whenCallCreate_thenPersistIt() {
        final var castMember = CastMember.newCastMember(VIN_DIESEL, CastMemberType.ACTOR, true);

        final var actualCastMember = this.gateway.create(castMember);

        assertEquals(castMember.getId(), actualCastMember.getId());
        assertEquals(1, this.castMemberRepository.count());
        assertEquals(CastMemberType.ACTOR, reload(castMember).getType());
    }

    @Test
    void givenPersistedCastMember_whenCallUpdate_thenReplaceNameAndType() {
        final var castMember = this.gateway.create(CastMember.newCastMember("Vin", CastMemberType.ACTOR, true));

        this.gateway.update(castMember.update(VIN_DIESEL, CastMemberType.DIRECTOR));

        final var actualCastMember = reload(castMember);
        assertEquals(VIN_DIESEL, actualCastMember.getName());
        assertEquals(CastMemberType.DIRECTOR, actualCastMember.getType());
    }

    @Test
    void givenPersistedCastMember_whenCallDeleteById_thenRemoveIt() {
        final var castMember = this.gateway.create(CastMember.newCastMember(VIN_DIESEL, CastMemberType.ACTOR, true));

        this.gateway.deleteById(castMember.getId());

        assertEquals(0, this.castMemberRepository.count());
    }

    @Test
    void givenUnknownId_whenCallDeleteById_thenDoNothing() {
        this.gateway.create(CastMember.newCastMember(VIN_DIESEL, CastMemberType.ACTOR, true));

        this.gateway.deleteById(CastMemberID.unique());

        assertEquals(1, this.castMemberRepository.count());
    }

    @Test
    void givenPersistedCastMember_whenCallFindById_thenReturnIt() {
        final var castMember = this.gateway.create(CastMember.newCastMember(VIN_DIESEL, CastMemberType.ACTOR, false));

        final var actualCastMember = this.gateway.findById(castMember.getId()).orElseThrow();

        assertEquals(VIN_DIESEL, actualCastMember.getName());
        assertFalse(actualCastMember.isActive());
        assertEquals(castMember.getCreatedAt(), actualCastMember.getCreatedAt());
    }

    @Test
    void givenUnknownId_whenCallFindById_thenReturnEmpty() {
        final var actualCastMember = this.gateway.findById(CastMemberID.unique());

        assertTrue(actualCastMember.isEmpty());
    }

    @Test
    void givenPersistedCastMembers_whenCallFindAll_thenReturnPaginatedByName() {
        givenPersisted(VIN_DIESEL, CastMemberType.ACTOR);
        givenPersisted(SPIELBERG, CastMemberType.DIRECTOR);

        final var actualPage = this.gateway.findAll(new SearchQuery(0, 10, null, NAME, ASC));

        assertEquals(2, actualPage.total());
        assertEquals(SPIELBERG, actualPage.items().getFirst().getName());
        assertEquals(VIN_DIESEL, actualPage.items().getLast().getName());
    }

    @Test
    void givenPersistedCastMembers_whenCallFindAllWithTerms_thenFilterByName() {
        givenPersisted(VIN_DIESEL, CastMemberType.ACTOR);
        givenPersisted(SPIELBERG, CastMemberType.DIRECTOR);

        final var actualPage = this.gateway.findAll(new SearchQuery(0, 10, "spiel", NAME, ASC));

        assertEquals(1, actualPage.total());
        assertEquals(SPIELBERG, actualPage.items().getFirst().getName());
    }

    @Test
    void givenPersistedCastMembers_whenCallFindAllOnSecondPage_thenReturnRemainingItems() {
        givenPersisted(VIN_DIESEL, CastMemberType.ACTOR);
        givenPersisted(SPIELBERG, CastMemberType.DIRECTOR);

        final var actualPage = this.gateway.findAll(new SearchQuery(1, 1, null, NAME, ASC));

        assertEquals(2, actualPage.total());
        assertEquals(1, actualPage.currentPage());
        assertEquals(VIN_DIESEL, actualPage.items().getFirst().getName());
    }

    @Test
    void givenNoCastMember_whenCallFindAll_thenReturnEmptyPage() {
        final var actualPage = this.gateway.findAll(new SearchQuery(0, 10, null, NAME, ASC));

        assertEquals(0, actualPage.total());
        assertTrue(actualPage.items().isEmpty());
    }

    private void givenPersisted(final String name, final CastMemberType type) {
        this.gateway.create(CastMember.newCastMember(name, type, true));
    }

    private CastMember reload(final CastMember castMember) {
        return this.gateway.findById(castMember.getId()).orElseThrow();
    }
}
