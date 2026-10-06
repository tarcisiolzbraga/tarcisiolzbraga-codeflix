package com.tarcisiolzbraga.codeflix.videos.application.video.delete;

import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoID;
import java.util.Objects;

// Idempotente, como os outros: a remoção chega por mensagem, que pode ser reentregue.
public class DefaultDeleteVideoUseCase extends DeleteVideoUseCase {

    private final VideoGateway videoGateway;

    public DefaultDeleteVideoUseCase(final VideoGateway videoGateway) {
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
    }

    @Override
    public void execute(final VideoID input) {
        Objects.requireNonNull(input, "'input' should not be null");
        this.videoGateway.deleteById(input);
    }
}
