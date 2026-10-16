package com.tarcisiolzbraga.codeflix.admin.application.castmember.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ListCastMembersUseCaseIT {

    private static final String VIN_DIESEL = "Vin Diesel";
    private static final String SPIELBERG = "Steven Spielberg";
    private static final String NAME = "name";
    private static final String ASC = "asc";

    @Autowired
    private ListCastMembersUseCase useCase;

    @Autowired
    private CastMemberGateway castMemberGateway;

    @Test
    void givenPersistedCastMembers_whenCallExecute_thenReturnThemSortedByName() {
        givenPersisted(VIN_DIESEL, CastMemberType.ACTOR);
        givenPersisted(SPIELBERG, CastMemberType.DIRECTOR);

        final var actualPage = this.useCase.execute(new SearchQuery(0, 10, null, NAME, ASC));

        assertEquals(2, actualPage.total());
        assertEquals(SPIELBERG, actualPage.items().getFirst().name());
        assertEquals("DIRECTOR", actualPage.items().getFirst().type());
        assertEquals(VIN_DIESEL, actualPage.items().getLast().name());
    }

    @Test
    void givenTerms_whenCallExecute_thenFilterByName() {
        givenPersisted(VIN_DIESEL, CastMemberType.ACTOR);
        givenPersisted(SPIELBERG, CastMemberType.DIRECTOR);

        final var actualPage = this.useCase.execute(new SearchQuery(0, 10, "diesel", NAME, ASC));

        assertEquals(1, actualPage.total());
        assertEquals(VIN_DIESEL, actualPage.items().getFirst().name());
    }

    @Test
    void givenSecondPage_whenCallExecute_thenReturnTheRemainingItems() {
        givenPersisted(VIN_DIESEL, CastMemberType.ACTOR);
        givenPersisted(SPIELBERG, CastMemberType.DIRECTOR);

        final var actualPage = this.useCase.execute(new SearchQuery(1, 1, null, NAME, ASC));

        assertEquals(2, actualPage.total());
        assertEquals(1, actualPage.currentPage());
        assertEquals(VIN_DIESEL, actualPage.items().getFirst().name());
    }

    @Test
    void givenNoCastMember_whenCallExecute_thenReturnEmptyPage() {
        final var actualPage = this.useCase.execute(new SearchQuery(0, 10, null, NAME, ASC));

        assertEquals(0, actualPage.total());
        assertTrue(actualPage.items().isEmpty());
    }

    private void givenPersisted(final String name, final CastMemberType type) {
        this.castMemberGateway.create(CastMember.newCastMember(name, type, true));
    }
}
