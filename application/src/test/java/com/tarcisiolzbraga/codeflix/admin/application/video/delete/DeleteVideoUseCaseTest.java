package com.tarcisiolzbraga.codeflix.admin.application.video.delete;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteVideoUseCaseTest {

    @Mock
    private VideoGateway videoGateway;

    @InjectMocks
    private DefaultDeleteVideoUseCase useCase;

    @Test
    void givenAnyId_whenCallExecute_thenDelegateToTheGateway() {
        final var expectedId = VideoID.unique();

        useCase.execute(expectedId.getValue());

        verify(videoGateway).deleteById(expectedId);
    }

    @Test
    void givenUnknownId_whenCallExecute_thenDoNotThrow() {
        final var expectedId = VideoID.unique();

        assertDoesNotThrow(() -> useCase.execute(expectedId.getValue()));

        verify(videoGateway).deleteById(expectedId);
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedException = new IllegalStateException("gateway indisponível");
        doThrow(expectedException).when(videoGateway).deleteById(any());

        final var actualException =
                assertThrows(IllegalStateException.class, () -> useCase.execute(VideoID.unique().getValue()));

        assertSame(expectedException, actualException);
    }
}
