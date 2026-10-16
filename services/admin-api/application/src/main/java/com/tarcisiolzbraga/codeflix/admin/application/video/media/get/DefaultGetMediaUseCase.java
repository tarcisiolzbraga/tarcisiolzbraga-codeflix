package com.tarcisiolzbraga.codeflix.admin.application.video.media.get;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaResourceGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import java.util.Objects;

// Não consulta o vídeo: o endereço do arquivo já é calculado do id e do tipo, então arquivo ausente
// é 404 sem custar uma ida ao banco.
public class DefaultGetMediaUseCase extends GetMediaUseCase {

    private final MediaResourceGateway mediaResourceGateway;

    public DefaultGetMediaUseCase(final MediaResourceGateway mediaResourceGateway) {
        this.mediaResourceGateway =
                Objects.requireNonNull(mediaResourceGateway, "'mediaResourceGateway' should not be null");
    }

    @Override
    public MediaOutput execute(final GetMediaCommand input) {
        final var id = VideoID.from(input.videoId());
        return this.mediaResourceGateway
                .getResource(id, input.type())
                .map(MediaOutput::from)
                .orElseThrow(() -> NotFoundException.withMedia(input.type().name(), id));
    }
}
