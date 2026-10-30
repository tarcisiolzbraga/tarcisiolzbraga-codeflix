package com.tarcisiolzbraga.codeflix.admin.application.video.media.update;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaStatus;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import java.util.Objects;

// Quem chama é o consumidor da fila do codificador, não o usuário: não há entrada para validar,
// então não há Either. Vídeo inexistente sobe como NotFoundException.
public class DefaultUpdateMediaStatusUseCase extends UpdateMediaStatusUseCase {

    private final VideoGateway videoGateway;

    public DefaultUpdateMediaStatusUseCase(final VideoGateway videoGateway) {
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
    }

    @Override
    public void execute(final UpdateMediaStatusCommand input) {
        if (input.status() == MediaStatus.PENDING) {
            return;
        }
        final var id = VideoID.from(input.videoId());
        final var video = this.videoGateway
                .findById(id)
                .orElseThrow(() -> NotFoundException.with(Video.class, id));
        move(video, input);
        this.videoGateway.update(video);
    }

    // Switch exaustivo, e não um `if` com `else`: o `else` mandava **tudo** que não era COMPLETED
    // para `processing`, e com FAILED isso gravaria a falha como "está codificando". Sem ramo
    // coringa, status novo no enum passa a ser erro de compilação aqui — que é onde se quer
    // descobrir, e não em produção.
    private void move(final Video video, final UpdateMediaStatusCommand input) {
        switch (input.status()) {
            case COMPLETED -> video.completed(input.type(), input.checksum(), input.encodedPath());
            case PROCESSING -> video.processing(input.type(), input.checksum());
            case FAILED -> video.failed(input.type(), input.checksum());
            case PENDING -> {
                // inalcançável: o execute devolve antes de chegar aqui
            }
        }
    }
}
