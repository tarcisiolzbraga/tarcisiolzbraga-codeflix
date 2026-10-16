package com.tarcisiolzbraga.codeflix.admin.application.castmember.deactivate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class DeactivateCastMemberUseCaseIT {

    private static final String EXPECTED_NAME = "Vin Diesel";

    @Autowired
    private DeactivateCastMemberUseCase useCase;

    @Autowired
    private CastMemberGateway castMemberGateway;

    @Test
    void givenActiveCastMember_whenCallExecute_thenPersistItAsInactive() {
        final var castMember = givenPersistedCastMember(true);

        this.useCase.execute(castMember.getId().getValue());

        assertFalse(reload(castMember).isActive());
    }

    @Test
    void givenInactiveCastMember_whenCallExecute_thenKeepItAsInactive() {
        final var castMember = givenPersistedCastMember(false);

        this.useCase.execute(castMember.getId().getValue());

        assertFalse(reload(castMember).isActive());
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
