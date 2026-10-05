package com.tarcisiolzbraga.codeflix.videos.application.castmember.delete;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteCastMemberUseCaseTest {

    private static final CastMemberID EXPECTED_ID = CastMemberID.from("7c1e3d4a-9b2f-4c80-8d61-a2b3c4d5e6f7");

    @Mock
    private CastMemberGateway castMemberGateway;

    @InjectMocks
    private DefaultDeleteCastMemberUseCase useCase;

    @Test
    void givenValidId_whenCallExecute_thenRemoveTheReplica() {
        assertDoesNotThrow(() -> useCase.execute(EXPECTED_ID));

        verify(castMemberGateway).deleteById(EXPECTED_ID);
    }

    @Test
    void givenTheSameIdTwice_whenCallExecute_thenRemoveTwiceWithoutComplaining() {
        useCase.execute(EXPECTED_ID);
        useCase.execute(EXPECTED_ID);

        verify(castMemberGateway, times(2)).deleteById(EXPECTED_ID);
    }

    @Test
    void givenNullId_whenCallExecute_thenThrowNullPointerExceptionAndNotTouchTheGateway() {
        final var actualException = assertThrows(NullPointerException.class, () -> useCase.execute(null));

        assertEquals("'input' should not be null", actualException.getMessage());
        verify(castMemberGateway, never()).deleteById(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedException = new IllegalStateException("catálogo indisponível");
        doThrow(expectedException).when(castMemberGateway).deleteById(EXPECTED_ID);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(EXPECTED_ID));

        assertSame(expectedException, actualException);
    }
}
