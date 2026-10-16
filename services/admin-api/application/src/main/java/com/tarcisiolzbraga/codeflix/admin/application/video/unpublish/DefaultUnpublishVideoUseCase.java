package com.tarcisiolzbraga.codeflix.admin.application.video.unpublish;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoOutput;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import java.util.Objects;

public class DefaultUnpublishVideoUseCase extends UnpublishVideoUseCase {

    private final VideoGateway videoGateway;

    public DefaultUnpublishVideoUseCase(final VideoGateway videoGateway) {
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
    }

    @Override
    public VideoOutput execute(final String input) {
        final var id = VideoID.from(input);
        final var video = this.videoGateway
                .findById(id)
                .orElseThrow(() -> NotFoundException.with(Video.class, id));
        video.unpublish();
        return VideoOutput.from(this.videoGateway.update(video));
    }
}
