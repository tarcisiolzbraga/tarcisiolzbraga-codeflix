package com.tarcisiolzbraga.codeflix.admin.application.castmember.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateCastMemberUseCaseTest {

    private static final String EXPECTED_NAME = "Vin Diesel";
    private static final String TYPE_MESSAGE = "'type' should be one of ACTOR, DIRECTOR";

    @Mock
    private CastMemberGateway castMemberGateway;

    @InjectMocks
    private DefaultCreateCastMemberUseCase useCase;

    @Test
    void givenValidCommand_whenCallExecute_thenReturnRightWithCastMemberId() {
        final var command = CreateCastMemberCommand.with(EXPECTED_NAME, "ACTOR", true);
        when(castMemberGateway.create(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        assertNotNull(actualResult.get().id());
        verify(castMemberGateway).create(argThat(member -> isCreatedFrom(member, CastMemberType.ACTOR, true)));
    }

    @Test
    void givenLowercaseType_whenCallExecute_thenAcceptIt() {
        final var command = CreateCastMemberCommand.with(EXPECTED_NAME, "director", false);
        when(castMemberGateway.create(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        verify(castMemberGateway).create(argThat(member -> isCreatedFrom(member, CastMemberType.DIRECTOR, false)));
    }

    @Test
    void givenUnknownType_whenCallExecute_thenReturnLeftWithASingleError() {
        final var command = CreateCastMemberCommand.with(EXPECTED_NAME, "SINGER", true);

        final var actualResult = useCase.execute(command);

        assertEquals(List.of(TYPE_MESSAGE), messagesOf(actualResult.getLeft().getErrors()));
        verify(castMemberGateway, never()).create(any());
    }

    @Test
    void givenNullNameAndNullType_whenCallExecute_thenReturnLeftWithBothErrors() {
        final var command = CreateCastMemberCommand.with(null, null, true);

        final var actualResult = useCase.execute(command);

        assertEquals(
                List.of("'name' should not be null", TYPE_MESSAGE),
                messagesOf(actualResult.getLeft().getErrors()));
        verify(castMemberGateway, never()).create(any());
    }

    private boolean isCreatedFrom(final CastMember member, final CastMemberType type, final boolean isActive) {
        return EXPECTED_NAME.equals(member.getName())
                && type == member.getType()
                && isActive == member.isActive()
                && member.getCreatedAt().equals(member.getUpdatedAt());
    }

    private List<String> messagesOf(final List<ValidationError> errors) {
        return errors.stream().map(ValidationError::message).toList();
    }
}
