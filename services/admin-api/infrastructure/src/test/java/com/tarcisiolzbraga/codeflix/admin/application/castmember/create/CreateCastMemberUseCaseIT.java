package com.tarcisiolzbraga.codeflix.admin.application.castmember.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.persistence.CastMemberRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class CreateCastMemberUseCaseIT {

    private static final String EXPECTED_NAME = "Vin Diesel";

    @Autowired
    private CreateCastMemberUseCase useCase;

    @Autowired
    private CastMemberRepository castMemberRepository;

    @MockitoSpyBean
    private CastMemberGateway castMemberGateway;

    @Test
    void givenValidCommand_whenCallExecute_thenPersistTheCastMember() {
        final var command = CreateCastMemberCommand.with(EXPECTED_NAME, "ACTOR", true);

        final var actualResult = this.useCase.execute(command);

        assertTrue(actualResult.isRight());
        final var persisted = reload(actualResult.get().id());
        assertEquals(EXPECTED_NAME, persisted.getName());
        assertEquals(CastMemberType.ACTOR, persisted.getType());
        assertTrue(persisted.isActive());
    }

    @Test
    void givenInactiveCommand_whenCallExecute_thenPersistItInactive() {
        final var command = CreateCastMemberCommand.with("Steven Spielberg", "director", false);

        final var actualResult = this.useCase.execute(command);

        final var persisted = reload(actualResult.get().id());
        assertFalse(persisted.isActive());
        assertEquals(CastMemberType.DIRECTOR, persisted.getType());
    }

    @Test
    void givenInvalidNameAndUnknownType_whenCallExecute_thenReturnBothErrorsAndPersistNothing() {
        final var command = CreateCastMemberCommand.with(null, "SINGER", true);

        final var actualResult = this.useCase.execute(command);

        assertTrue(actualResult.isLeft());
        assertEquals(
                List.of("'name' should not be null", "'type' should be one of ACTOR, DIRECTOR"),
                actualResult.getLeft().getErrors().stream()
                        .map(ValidationError::message)
                        .toList());
        assertEquals(0, this.castMemberRepository.count());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheExceptionInsteadOfValidationError() {
        final var command = CreateCastMemberCommand.with(EXPECTED_NAME, "ACTOR", true);
        final var expectedException = new IllegalStateException("banco indisponível");
        doThrow(expectedException).when(this.castMemberGateway).create(any());

        final var actualException = assertThrows(IllegalStateException.class, () -> this.useCase.execute(command));

        assertSame(expectedException, actualException);
        assertEquals(0, this.castMemberRepository.count());
    }

    private CastMember reload(final String id) {
        return this.castMemberGateway.findById(CastMemberID.from(id)).orElseThrow();
    }
}
