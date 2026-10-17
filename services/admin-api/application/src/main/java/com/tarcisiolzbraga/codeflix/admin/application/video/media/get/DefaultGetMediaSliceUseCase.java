package com.tarcisiolzbraga.codeflix.admin.application.video.media.get;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaResourceGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import java.util.Objects;

// Não consulta o vídeo, pelo mesmo motivo do GetMediaUseCase: o endereço do arquivo é calculado do id
// e do tipo, então arquivo ausente é 404 sem custar uma ida ao banco.
public class DefaultGetMediaSliceUseCase extends GetMediaSliceUseCase {

    private final MediaResourceGateway mediaResourceGateway;

    public DefaultGetMediaSliceUseCase(final MediaResourceGateway mediaResourceGateway) {
        this.mediaResourceGateway =
                Objects.requireNonNull(mediaResourceGateway, "'mediaResourceGateway' should not be null");
    }

    @Override
    public MediaSliceOutput execute(final GetMediaSliceCommand input) {
        final var id = VideoID.from(input.videoId());
        return this.mediaResourceGateway
                .getResourceSlice(id, input.type(), input.range())
                .map(MediaSliceOutput::from)
                .orElseThrow(() -> NotFoundException.withMedia(input.type().name(), id));
    }
}
