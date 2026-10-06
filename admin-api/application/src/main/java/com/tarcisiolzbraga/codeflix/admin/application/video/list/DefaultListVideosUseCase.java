package com.tarcisiolzbraga.codeflix.admin.application.video.list;

import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoSearchQuery;
import java.util.Objects;

public class DefaultListVideosUseCase extends ListVideosUseCase {

    private final VideoGateway videoGateway;

    public DefaultListVideosUseCase(final VideoGateway videoGateway) {
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
    }

    @Override
    public Pagination<VideoListOutput> execute(final VideoSearchQuery input) {
        return this.videoGateway.findAll(input).map(VideoListOutput::from);
    }
}
