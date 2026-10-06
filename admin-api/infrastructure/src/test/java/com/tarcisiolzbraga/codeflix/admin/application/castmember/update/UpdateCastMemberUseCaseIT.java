package com.tarcisiolzbraga.codeflix.admin.application.castmember.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class UpdateCastMemberUseCaseIT {

    private static final String OLD_NAME = "Vin";
    private static final String EXPECTED_NAME = "Vin Diesel";

    @Autowired
    private UpdateCastMemberUseCase useCase;

    @MockitoSpyBean
    private CastMemberGateway castMemberGateway;

    @Test
    void givenPersistedCastMember_whenCallExecute_thenReplaceNameAndType() {
        final var castMember = givenPersistedCastMember();
        final var command =
                UpdateCastMemberCommand.with(castMember.getId().getValue(), EXPECTED_NAME, "DIRECTOR");

        final var actualResult = this.useCase.execute(command);

        assertTrue(actualResult.isRight());
        final var persisted = reload(castMember);
        assertEquals(EXPECTED_NAME, persisted.getName());
        assertEquals(CastMemberType.DIRECTOR, persisted.getType());
    }

    @Test
    void givenPersistedCastMember_whenCallExecute_thenKeepCreatedAtAndAdvanceUpdatedAt() {
        final var castMember = givenPersistedCastMember();
        final var command = UpdateCastMemberCommand.with(castMember.getId().getValue(), EXPECTED_NAME, "ACTOR");

        this.useCase.execute(command);

        final var persisted = reload(castMember);
        assertEquals(castMember.getCreatedAt(), persisted.getCreatedAt());
        assertTrue(castMember.getUpdatedAt().isBefore(persisted.getUpdatedAt()));
    }

    @Test
    void givenInvalidNameAndUnknownType_whenCallExecute_thenReturnBothErrorsAndKeepTheStoredValues() {
        final var castMember = givenPersistedCastMember();
        final var command = UpdateCastMemberCommand.with(castMember.getId().getValue(), null, "SINGER");

        final var actualResult = this.useCase.execute(command);

        assertEquals(
                List.of("'name' should not be null", "'type' should be one of ACTOR, DIRECTOR"),
                actualResult.getLeft().getErrors().stream()
                        .map(ValidationError::message)
                        .toList());
        assertEquals(OLD_NAME, reload(castMember).getName());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = CastMemberID.unique();
        final var command = UpdateCastMemberCommand.with(expectedId.getValue(), EXPECTED_NAME, "ACTOR");

        final var actualException = assertThrows(NotFoundException.class, () -> this.useCase.execute(command));

        assertEquals(
                "CastMember with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheExceptionInsteadOfValidationError() {
        final var castMember = givenPersistedCastMember();
        final var command = UpdateCastMemberCommand.with(castMember.getId().getValue(), EXPECTED_NAME, "ACTOR");
        final var expectedException = new IllegalStateException("banco indisponível");
        doThrow(expectedException).when(this.castMemberGateway).update(any());

        final var actualException = assertThrows(IllegalStateException.class, () -> this.useCase.execute(command));

        assertSame(expectedException, actualException);
    }

    private CastMember givenPersistedCastMember() {
        return this.castMemberGateway.create(CastMember.newCastMember(OLD_NAME, CastMemberType.ACTOR, true));
    }

    private CastMember reload(final CastMember castMember) {
        return this.castMemberGateway.findById(castMember.getId()).orElseThrow();
    }
}
