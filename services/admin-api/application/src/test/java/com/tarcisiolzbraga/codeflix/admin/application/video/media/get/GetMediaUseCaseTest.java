package com.tarcisiolzbraga.codeflix.admin.application.video.media.get;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaResourceGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaContent;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Resource;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetMediaUseCaseTest {

    @Mock
    private MediaResourceGateway mediaResourceGateway;

    @InjectMocks
    private DefaultGetMediaUseCase useCase;

    @Test
    void givenAStoredMedia_whenCallExecute_thenReceiveItsContentAndHowToServeIt() {
        final var id = VideoID.unique();
        final var resource = Resource.with("conteudo".getBytes(), "abc1", "video/mp4", "duna.mp4");
        when(mediaResourceGateway.getResource(id, VideoMediaType.VIDEO)).thenReturn(Optional.of(resource));

        final var actualOutput = useCase.execute(new GetMediaCommand(id.getValue(), VideoMediaType.VIDEO));

        assertEquals("conteudo", writtenBy(actualOutput.content()));
        assertEquals("video/mp4", actualOutput.contentType());
        assertEquals("duna.mp4", actualOutput.name());
    }

    @Test
    void givenNoStoredMedia_whenCallExecute_thenThrowNotFound() {
        final var id = VideoID.unique();
        when(mediaResourceGateway.getResource(id, VideoMediaType.BANNER)).thenReturn(Optional.empty());
        final var command = new GetMediaCommand(id.getValue(), VideoMediaType.BANNER);

        final var actualException = assertThrows(NotFoundException.class, () -> useCase.execute(command));

        assertEquals(
                "Media BANNER of Video with ID %s was not found".formatted(id.getValue()),
                actualException.getMessage());
    }

    @Test
    void givenFailingStorage_whenCallExecute_thenPropagateTheException() {
        final var id = VideoID.unique();
        final var expectedException = new IllegalStateException("armazenamento indisponível");
        when(mediaResourceGateway.getResource(id, VideoMediaType.VIDEO)).thenThrow(expectedException);
        final var command = new GetMediaCommand(id.getValue(), VideoMediaType.VIDEO);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        assertSame(expectedException, actualException);
    }

    // O teste de cópia defensiva saiu junto com a cópia: o conteúdo é uma função que escreve, e não
    // um arranjo que quem recebeu poderia alterar. Em troca, este afirma que a função pode ser
    // escrita e entrega o que prometeu — o que a anterior não cobria.
    @Test
    void givenAStoredMedia_whenTheContentIsWritten_thenItDeliversWhatItPromised() {
        final var id = VideoID.unique();
        final var resource = Resource.with(new byte[] {1, 2, 3}, "abc1", "video/mp4", "duna.mp4");
        when(mediaResourceGateway.getResource(id, VideoMediaType.VIDEO)).thenReturn(Optional.of(resource));

        final var actualOutput = useCase.execute(new GetMediaCommand(id.getValue(), VideoMediaType.VIDEO));

        final var destination = new ByteArrayOutputStream();
        try {
            actualOutput.content().writeTo(destination);
        } catch (final IOException failure) {
            throw new IllegalStateException(failure);
        }
        assertArrayEquals(new byte[] {1, 2, 3}, destination.toByteArray());
    }

    private static String writtenBy(final MediaContent content) {
        final var destination = new ByteArrayOutputStream();
        try {
            content.writeTo(destination);
        } catch (final IOException failure) {
            throw new IllegalStateException(failure);
        }
        return destination.toString(StandardCharsets.UTF_8);
    }
}
