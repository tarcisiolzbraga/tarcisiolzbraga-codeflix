package com.tarcisiolzbraga.codeflix.admin.application.castmember.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
class GetCastMemberByIdUseCaseTest {

    private static final String EXPECTED_NAME = "Vin Diesel";

    @Mock
    private CastMemberGateway castMemberGateway;

    @InjectMocks
    private DefaultGetCastMemberByIdUseCase useCase;

    @Test
    void givenExistingId_whenCallExecute_thenReturnTheCastMember() {
        final var castMember = CastMember.newCastMember(EXPECTED_NAME, CastMemberType.ACTOR, true);
        when(castMemberGateway.findById(any())).thenReturn(Optional.of(castMember));

        final var actualOutput = useCase.execute(castMember.getId().getValue());

        assertEquals(castMember.getId().getValue(), actualOutput.id());
        assertEquals(EXPECTED_NAME, actualOutput.name());
        assertEquals("ACTOR", actualOutput.type());
        assertTrue(actualOutput.isActive());
        assertEquals(castMember.getCreatedAt(), actualOutput.createdAt());
        assertEquals(castMember.getUpdatedAt(), actualOutput.updatedAt());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = CastMemberID.unique();
        when(castMemberGateway.findById(any())).thenReturn(Optional.empty());

        final var actualException =
                assertThrows(NotFoundException.class, () -> useCase.execute(expectedId.getValue()));

        assertEquals(
                "CastMember with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }
}
