package com.tarcisiolzbraga.codeflix.admin.application.castmember.delete;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteCastMemberUseCaseTest {

    @Mock
    private CastMemberGateway castMemberGateway;

    @Mock
    private VideoGateway videoGateway;

    @InjectMocks
    private DefaultDeleteCastMemberUseCase useCase;

    @Test
    void givenAnyId_whenCallExecute_thenDelegateToTheGateway() {
        final var expectedId = CastMemberID.unique();

        useCase.execute(expectedId.getValue());

        verify(castMemberGateway).deleteById(expectedId);
    }

    @Test
    void givenUnknownId_whenCallExecute_thenDoNotThrow() {
        final var expectedId = CastMemberID.unique();

        assertDoesNotThrow(() -> useCase.execute(expectedId.getValue()));

        verify(castMemberGateway).deleteById(expectedId);
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedException = new IllegalStateException("gateway indisponível");
        doThrow(expectedException).when(castMemberGateway).deleteById(any());

        final var actualException = assertThrows(
                IllegalStateException.class, () -> useCase.execute(CastMemberID.unique().getValue()));

        assertSame(expectedException, actualException);
    }
    @Test
    void givenCastMemberLinkedToAVideo_whenCallExecute_thenThrowConflictAndKeepIt() {
        final var expectedId = CastMemberID.unique();
        when(videoGateway.existsByCastMember(expectedId)).thenReturn(true);

        final var actualException =
                assertThrows(ConflictException.class, () -> useCase.execute(expectedId.getValue()));

        assertEquals(
                "CastMember with ID %s is linked to at least one Video; deactivate it instead"
                        .formatted(expectedId.getValue()),
                actualException.getMessage());
        verify(castMemberGateway, never()).deleteById(any());
    }
}
