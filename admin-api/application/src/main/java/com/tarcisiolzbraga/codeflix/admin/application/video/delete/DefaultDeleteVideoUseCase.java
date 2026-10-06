package com.tarcisiolzbraga.codeflix.admin.application.video.delete;

import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaResourceGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import java.util.Objects;

// O registro sai primeiro: se ele não sair, os arquivos ficam onde estão, porque um vídeo sem os
// próprios arquivos é pior do que um arquivo órfão no armazenamento.
public class DefaultDeleteVideoUseCase extends DeleteVideoUseCase {

    private final VideoGateway videoGateway;
    private final MediaResourceGateway mediaResourceGateway;

    public DefaultDeleteVideoUseCase(
            final VideoGateway videoGateway, final MediaResourceGateway mediaResourceGateway) {
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
        this.mediaResourceGateway =
                Objects.requireNonNull(mediaResourceGateway, "'mediaResourceGateway' should not be null");
    }

    @Override
    public void execute(final String input) {
        final var id = VideoID.from(input);
        this.videoGateway.deleteById(id);
        this.mediaResourceGateway.clearResources(id);
    }
}
