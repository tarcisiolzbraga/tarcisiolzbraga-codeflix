package com.tarcisiolzbraga.codeflix.admin.application.video.delete;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaResourceGateway;
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

    @Mock
    private MediaResourceGateway mediaResourceGateway;

    @InjectMocks
    private DefaultDeleteVideoUseCase useCase;

    @Test
    void givenAnyId_whenCallExecute_thenDelegateToTheGateway() {
        final var expectedId = VideoID.unique();

        useCase.execute(expectedId.getValue());

        verify(videoGateway).deleteById(expectedId);
    }

    @Test
    void givenAnyId_whenCallExecute_thenAlsoRemoveTheFilesOfTheVideo() {
        final var expectedId = VideoID.unique();

        useCase.execute(expectedId.getValue());

        verify(mediaResourceGateway).clearResources(expectedId);
    }

    @Test
    void givenFailingVideoGateway_whenCallExecute_thenKeepTheFiles() {
        final var expectedId = VideoID.unique();
        doThrow(new IllegalStateException("gateway indisponível")).when(videoGateway).deleteById(any());

        assertThrows(IllegalStateException.class, () -> useCase.execute(expectedId.getValue()));

        verify(mediaResourceGateway, never()).clearResources(any());
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
