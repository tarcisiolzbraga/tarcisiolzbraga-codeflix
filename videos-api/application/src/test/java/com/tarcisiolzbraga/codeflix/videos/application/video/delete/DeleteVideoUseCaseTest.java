package com.tarcisiolzbraga.codeflix.videos.application.video.delete;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteVideoUseCaseTest {

    private static final VideoID EXPECTED_ID = VideoID.from("9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608");

    @Mock
    private VideoGateway videoGateway;

    @InjectMocks
    private DefaultDeleteVideoUseCase useCase;

    @Test
    void givenValidId_whenCallExecute_thenRemoveTheReplica() {
        assertDoesNotThrow(() -> useCase.execute(EXPECTED_ID));

        verify(videoGateway).deleteById(EXPECTED_ID);
    }

    @Test
    void givenTheSameIdTwice_whenCallExecute_thenRemoveTwiceWithoutComplaining() {
        useCase.execute(EXPECTED_ID);
        useCase.execute(EXPECTED_ID);

        verify(videoGateway, times(2)).deleteById(EXPECTED_ID);
    }

    @Test
    void givenNullId_whenCallExecute_thenThrowNullPointerExceptionAndNotTouchTheGateway() {
        final var actualException = assertThrows(NullPointerException.class, () -> useCase.execute(null));

        assertEquals("'input' should not be null", actualException.getMessage());
        verify(videoGateway, never()).deleteById(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedException = new IllegalStateException("catálogo indisponível");
        doThrow(expectedException).when(videoGateway).deleteById(EXPECTED_ID);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(EXPECTED_ID));

        assertSame(expectedException, actualException);
    }
}
