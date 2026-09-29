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

    private void move(final Video video, final UpdateMediaStatusCommand input) {
        if (input.status() == MediaStatus.COMPLETED) {
            video.completed(input.type(), input.encodedPath());
        } else {
            video.processing(input.type());
        }
    }
}
