package com.tarcisiolzbraga.codeflix.videos.application.castmember.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaveCastMemberUseCaseTest {

    private static final String EXPECTED_ID = "7c1e3d4a-9b2f-4c80-8d61-a2b3c4d5e6f7";
    private static final String EXPECTED_NAME = "Denis Villeneuve";
    private static final Instant EXPECTED_CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant EXPECTED_UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    @Mock
    private CastMemberGateway castMemberGateway;

    @InjectMocks
    private DefaultSaveCastMemberUseCase useCase;

    @Test
    void givenValidCommand_whenCallExecute_thenStoreTheReplicaAndReturnItsId() {
        final var command = aCommand(EXPECTED_NAME, "DIRECTOR");
        when(castMemberGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var actualOutput = useCase.execute(command);

        assertEquals(EXPECTED_ID, actualOutput.id());
        final var captor = ArgumentCaptor.forClass(CastMember.class);
        verify(castMemberGateway).save(captor.capture());
        final var actualMember = captor.getValue();
        assertEquals(EXPECTED_NAME, actualMember.getName());
        assertEquals(CastMemberType.DIRECTOR, actualMember.getType());
        assertEquals(EXPECTED_CREATED_AT, actualMember.getCreatedAt());
        assertEquals(EXPECTED_UPDATED_AT, actualMember.getUpdatedAt());
    }

    @Test
    void givenTheTypeInAnotherCase_whenCallExecute_thenStillConvertIt() {
        final var command = aCommand(EXPECTED_NAME, "actor");
        when(castMemberGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command);

        final var captor = ArgumentCaptor.forClass(CastMember.class);
        verify(castMemberGateway).save(captor.capture());
        assertEquals(CastMemberType.ACTOR, captor.getValue().getType());
    }

    @Test
    void givenAnUnknownType_whenCallExecute_thenThrowDomainExceptionNamingTheTypesThatExist() {
        final var command = aCommand(EXPECTED_NAME, "PRODUTOR");

        final var actualException = assertThrows(DomainException.class, () -> useCase.execute(command));

        assertEquals("'type' should be one of ACTOR, DIRECTOR", actualException.getMessage());
        verify(castMemberGateway, never()).save(any());
    }

    @Test
    void givenNullId_whenCallExecute_thenThrowDomainExceptionAndNotTouchTheGateway() {
        final var command = new SaveCastMemberCommand(
                null, EXPECTED_NAME, "ACTOR", true, EXPECTED_CREATED_AT, EXPECTED_UPDATED_AT);

        final var actualException = assertThrows(DomainException.class, () -> useCase.execute(command));

        assertEquals("'id' should not be null", actualException.getMessage());
        verify(castMemberGateway, never()).save(any());
    }

    @Test
    void givenBlankName_whenCallExecute_thenThrowDomainExceptionAndNotTouchTheGateway() {
        final var command = aCommand("  ", "ACTOR");

        final var actualException = assertThrows(DomainException.class, () -> useCase.execute(command));

        assertEquals("'name' should not be empty", actualException.getMessage());
        verify(castMemberGateway, never()).save(any());
    }

    @Test
    void givenNullCommand_whenCallExecute_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> useCase.execute(null));

        assertEquals("'input' should not be null", actualException.getMessage());
        verify(castMemberGateway, never()).save(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var command = aCommand(EXPECTED_NAME, "ACTOR");
        final var expectedException = new IllegalStateException("catálogo indisponível");
        when(castMemberGateway.save(any())).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        assertSame(expectedException, actualException);
    }

    private static SaveCastMemberCommand aCommand(final String name, final String type) {
        return new SaveCastMemberCommand(
                EXPECTED_ID, name, type, true, EXPECTED_CREATED_AT, EXPECTED_UPDATED_AT);
    }
}
