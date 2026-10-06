package com.tarcisiolzbraga.codeflix.admin.application.video.media.upload;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaResourceGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoResource;
import java.util.Objects;

// Sem Either: não há nada do usuário para validar aqui. O tipo da mídia já chega convertido, e
// vídeo inexistente é 404, não erro de validação.
public class DefaultUploadMediaUseCase extends UploadMediaUseCase {

    private final VideoGateway videoGateway;
    private final MediaResourceGateway mediaResourceGateway;

    public DefaultUploadMediaUseCase(
            final VideoGateway videoGateway, final MediaResourceGateway mediaResourceGateway) {
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
        this.mediaResourceGateway =
                Objects.requireNonNull(mediaResourceGateway, "'mediaResourceGateway' should not be null");
    }

    @Override
    public UploadMediaOutput execute(final UploadMediaCommand input) {
        final var id = VideoID.from(input.videoId());
        final var video = this.videoGateway
                .findById(id)
                .orElseThrow(() -> NotFoundException.with(Video.class, id));
        attach(video, id, input.resource());
        this.videoGateway.update(video);
        return new UploadMediaOutput(id.getValue(), input.resource().type());
    }

    // O tipo escolhe o método de intenção do agregado, um para cada arquivo.
    private void attach(final Video video, final VideoID id, final VideoResource resource) {
        switch (resource.type()) {
            case VIDEO -> video.updateVideoMedia(this.mediaResourceGateway.storeAudioVideo(id, resource));
            case TRAILER -> video.updateTrailerMedia(this.mediaResourceGateway.storeAudioVideo(id, resource));
            case BANNER -> video.updateBanner(this.mediaResourceGateway.storeImage(id, resource));
            case THUMBNAIL -> video.updateThumbnail(this.mediaResourceGateway.storeImage(id, resource));
            case THUMBNAIL_HALF ->
                video.updateThumbnailHalf(this.mediaResourceGateway.storeImage(id, resource));
        }
    }
}
