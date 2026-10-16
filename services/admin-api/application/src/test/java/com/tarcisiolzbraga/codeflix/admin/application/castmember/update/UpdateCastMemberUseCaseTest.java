package com.tarcisiolzbraga.codeflix.admin.application.castmember.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateCastMemberUseCaseTest {

    private static final String EXPECTED_NAME = "Vin Diesel";
    private static final String TYPE_MESSAGE = "'type' should be one of ACTOR, DIRECTOR";

    @Mock
    private CastMemberGateway castMemberGateway;

    @InjectMocks
    private DefaultUpdateCastMemberUseCase useCase;

    @Test
    void givenValidCommand_whenCallExecute_thenReplaceNameAndType() {
        final var castMember = givenStoredCastMember();
        final var command = UpdateCastMemberCommand.with(castMember.getId().getValue(), EXPECTED_NAME, "DIRECTOR");
        when(castMemberGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        verify(castMemberGateway)
                .update(argThat(member -> EXPECTED_NAME.equals(member.getName())
                        && member.getType() == CastMemberType.DIRECTOR));
    }

    @Test
    void givenUnknownType_whenCallExecute_thenReturnLeftWithoutUpdating() {
        final var castMember = givenStoredCastMember();
        final var command = UpdateCastMemberCommand.with(castMember.getId().getValue(), EXPECTED_NAME, "SINGER");

        final var actualResult = useCase.execute(command);

        assertEquals(List.of(TYPE_MESSAGE), messagesOf(actualResult.getLeft().getErrors()));
        verify(castMemberGateway, never()).update(any());
    }

    @Test
    void givenNullNameAndNullType_whenCallExecute_thenReturnLeftWithBothErrors() {
        final var castMember = givenStoredCastMember();
        final var command = UpdateCastMemberCommand.with(castMember.getId().getValue(), null, null);

        final var actualResult = useCase.execute(command);

        assertEquals(
                List.of("'name' should not be null", TYPE_MESSAGE),
                messagesOf(actualResult.getLeft().getErrors()));
        verify(castMemberGateway, never()).update(any());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = CastMemberID.unique();
        final var command = UpdateCastMemberCommand.with(expectedId.getValue(), EXPECTED_NAME, "ACTOR");
        when(castMemberGateway.findById(any())).thenReturn(Optional.empty());

        final var actualException = assertThrows(NotFoundException.class, () -> useCase.execute(command));

        assertEquals(
                "CastMember with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }

    private CastMember givenStoredCastMember() {
        final var castMember = CastMember.newCastMember("Vin", CastMemberType.ACTOR, true);
        when(castMemberGateway.findById(any())).thenReturn(Optional.of(castMember));
        return castMember;
    }

    private List<String> messagesOf(final List<ValidationError> errors) {
        return errors.stream().map(ValidationError::message).toList();
    }
}
