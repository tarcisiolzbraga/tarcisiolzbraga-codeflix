package com.tarcisiolzbraga.codeflix.admin.application.castmember.activate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivateCastMemberUseCaseTest {

    private static final String EXPECTED_NAME = "Vin Diesel";

    @Mock
    private CastMemberGateway castMemberGateway;

    @InjectMocks
    private DefaultActivateCastMemberUseCase useCase;

    @Test
    void givenInactiveCastMember_whenCallExecute_thenReturnItAsActive() {
        final var castMember = givenStoredCastMember(false);
        final var previousUpdatedAt = castMember.getUpdatedAt();
        when(castMemberGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualOutput = useCase.execute(castMember.getId().getValue());

        assertEquals(castMember.getId().getValue(), actualOutput.id());
        assertEquals(EXPECTED_NAME, actualOutput.name());
        assertTrue(actualOutput.isActive());
        assertTrue(actualOutput.updatedAt().isAfter(previousUpdatedAt));
    }

    @Test
    void givenActiveCastMember_whenCallExecute_thenKeepItAsActive() {
        final var castMember = givenStoredCastMember(true);
        when(castMemberGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualOutput = useCase.execute(castMember.getId().getValue());

        assertTrue(actualOutput.isActive());
        verify(castMemberGateway).update(castMember);
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFoundAndNotUpdate() {
        final var expectedId = CastMemberID.unique();
        when(castMemberGateway.findById(expectedId)).thenReturn(Optional.empty());

        final var actualException =
                assertThrows(NotFoundException.class, () -> useCase.execute(expectedId.getValue()));

        assertEquals(
                "CastMember with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
        verify(castMemberGateway, never()).update(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var castMember = givenStoredCastMember(false);
        final var expectedException = new IllegalStateException("gateway indisponível");
        when(castMemberGateway.update(any())).thenThrow(expectedException);

        final var actualException =
                assertThrows(IllegalStateException.class, () -> useCase.execute(castMember.getId().getValue()));

        assertSame(expectedException, actualException);
    }

    private CastMember givenStoredCastMember(final boolean isActive) {
        final var castMember = CastMember.newCastMember(EXPECTED_NAME, CastMemberType.ACTOR, isActive);
        when(castMemberGateway.findById(castMember.getId())).thenReturn(Optional.of(castMember));
        return castMember;
    }
}
