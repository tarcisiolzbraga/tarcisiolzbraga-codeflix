package com.tarcisiolzbraga.codeflix.admin.application.video.get;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoOutput;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import java.util.Objects;

public class DefaultGetVideoByIdUseCase extends GetVideoByIdUseCase {

    private final VideoGateway videoGateway;

    public DefaultGetVideoByIdUseCase(final VideoGateway videoGateway) {
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
    }

    @Override
    public VideoOutput execute(final String input) {
        final var id = VideoID.from(input);
        return this.videoGateway
                .findById(id)
                .map(VideoOutput::from)
                .orElseThrow(() -> NotFoundException.with(Video.class, id));
    }
}
