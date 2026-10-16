package com.tarcisiolzbraga.codeflix.admin.application.video.media.get;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaResourceGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Resource;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoResource;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class GetMediaUseCaseIT {

    @Autowired
    private GetMediaUseCase useCase;

    @MockitoSpyBean
    private MediaResourceGateway mediaResourceGateway;

    @Test
    void givenAStoredMedia_whenCallExecute_thenReceiveTheFileBack() {
        final var id = VideoID.unique();
        final var resource = Resource.with("conteudo do arquivo".getBytes(), "abc1", "video/mp4", "duna.mp4");
        this.mediaResourceGateway.storeAudioVideo(id, VideoResource.with(VideoMediaType.VIDEO, resource));

        final var actualOutput = useCase.execute(new GetMediaCommand(id.getValue(), VideoMediaType.VIDEO));

        assertArrayEquals("conteudo do arquivo".getBytes(), actualOutput.content());
        assertEquals("video/mp4", actualOutput.contentType());
        assertEquals("duna.mp4", actualOutput.name());
    }

    @Test
    void givenNothingStored_whenCallExecute_thenThrowNotFound() {
        final var id = VideoID.unique();
        final var command = new GetMediaCommand(id.getValue(), VideoMediaType.TRAILER);

        final var actualException = assertThrows(NotFoundException.class, () -> useCase.execute(command));

        assertEquals(
                "Media TRAILER of Video with ID %s was not found".formatted(id.getValue()),
                actualException.getMessage());
    }

    @Test
    void givenFailingStorage_whenCallExecute_thenPropagateTheException() {
        final var expectedException = new IllegalStateException("armazenamento indisponível");
        doThrow(expectedException).when(this.mediaResourceGateway).getResource(any(), any());
        final var command = new GetMediaCommand(VideoID.unique().getValue(), VideoMediaType.VIDEO);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        assertSame(expectedException, actualException);
    }
}
