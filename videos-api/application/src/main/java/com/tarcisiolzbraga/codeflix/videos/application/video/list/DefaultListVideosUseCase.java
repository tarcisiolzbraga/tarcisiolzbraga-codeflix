package com.tarcisiolzbraga.codeflix.videos.application.video.list;

import com.tarcisiolzbraga.codeflix.videos.application.video.VideoOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoSearchQuery;
import java.util.Objects;

public class DefaultListVideosUseCase extends ListVideosUseCase {

    private final VideoGateway videoGateway;

    public DefaultListVideosUseCase(final VideoGateway videoGateway) {
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
    }

    @Override
    public Pagination<VideoOutput> execute(final VideoSearchQuery input) {
        Objects.requireNonNull(input, "'input' should not be null");
        return this.videoGateway.findAll(input).map(VideoOutput::from);
    }
}
