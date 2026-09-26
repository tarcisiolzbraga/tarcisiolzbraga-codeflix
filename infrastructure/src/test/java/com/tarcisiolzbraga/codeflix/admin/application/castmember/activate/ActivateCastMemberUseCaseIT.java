package com.tarcisiolzbraga.codeflix.admin.application.castmember.activate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ActivateCastMemberUseCaseIT {

    private static final String EXPECTED_NAME = "Vin Diesel";

    @Autowired
    private ActivateCastMemberUseCase useCase;

    @Autowired
    private CastMemberGateway castMemberGateway;

    @Test
    void givenInactiveCastMember_whenCallExecute_thenPersistItAsActive() {
        final var castMember = givenPersistedCastMember(false);

        this.useCase.execute(castMember.getId().getValue());

        assertTrue(reload(castMember).isActive());
    }

    @Test
    void givenActiveCastMember_whenCallExecute_thenKeepItAsActive() {
        final var castMember = givenPersistedCastMember(true);

        this.useCase.execute(castMember.getId().getValue());

        assertTrue(reload(castMember).isActive());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = CastMemberID.unique();

        final var actualException =
                assertThrows(NotFoundException.class, () -> this.useCase.execute(expectedId.getValue()));

        assertEquals(
                "CastMember with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }

    private CastMember givenPersistedCastMember(final boolean isActive) {
        return this.castMemberGateway.create(
                CastMember.newCastMember(EXPECTED_NAME, CastMemberType.ACTOR, isActive));
    }

    private CastMember reload(final CastMember castMember) {
        return this.castMemberGateway.findById(castMember.getId()).orElseThrow();
    }
}
