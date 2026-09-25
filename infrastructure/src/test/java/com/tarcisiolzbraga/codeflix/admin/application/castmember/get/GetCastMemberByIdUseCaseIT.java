package com.tarcisiolzbraga.codeflix.admin.application.castmember.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
class GetCastMemberByIdUseCaseIT {

    private static final String EXPECTED_NAME = "Vin Diesel";

    @Autowired
    private GetCastMemberByIdUseCase useCase;

    @Autowired
    private CastMemberGateway castMemberGateway;

    @Test
    void givenPersistedCastMember_whenCallExecute_thenReturnItWithTimestamps() {
        final var castMember =
                this.castMemberGateway.create(CastMember.newCastMember(EXPECTED_NAME, CastMemberType.ACTOR, true));

        final var actualOutput = this.useCase.execute(castMember.getId().getValue());

        assertEquals(castMember.getId().getValue(), actualOutput.id());
        assertEquals(EXPECTED_NAME, actualOutput.name());
        assertEquals("ACTOR", actualOutput.type());
        assertTrue(actualOutput.isActive());
        assertEquals(castMember.getCreatedAt(), actualOutput.createdAt());
        assertEquals(castMember.getUpdatedAt(), actualOutput.updatedAt());
    }

    @Test
    void givenInactiveCastMember_whenCallExecute_thenReturnItInactive() {
        final var castMember = this.castMemberGateway.create(
                CastMember.newCastMember("Steven Spielberg", CastMemberType.DIRECTOR, false));

        final var actualOutput = this.useCase.execute(castMember.getId().getValue());

        assertFalse(actualOutput.isActive());
        assertEquals("DIRECTOR", actualOutput.type());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = CastMemberID.unique();

        final var actualException =
                assertThrows(NotFoundException.class, () -> this.useCase.execute(expectedId.getValue()));

        assertEquals(
                "CastMember with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }
}
