package com.tarcisiolzbraga.codeflix.videos.application.video.get;

import com.tarcisiolzbraga.codeflix.videos.application.video.VideoOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoID;
import java.util.Objects;
import java.util.Optional;

public class DefaultGetVideoUseCase extends GetVideoUseCase {

    private final VideoGateway videoGateway;

    public DefaultGetVideoUseCase(final VideoGateway videoGateway) {
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
    }

    @Override
    public Optional<VideoOutput> execute(final VideoID input) {
        Objects.requireNonNull(input, "'input' should not be null");
        return this.videoGateway.findById(input).map(VideoOutput::from);
    }
}
