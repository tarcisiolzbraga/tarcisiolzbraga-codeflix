package com.tarcisiolzbraga.codeflix.admin.application.castmember.delete;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteCastMemberUseCaseTest {

    @Mock
    private CastMemberGateway castMemberGateway;

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
}
