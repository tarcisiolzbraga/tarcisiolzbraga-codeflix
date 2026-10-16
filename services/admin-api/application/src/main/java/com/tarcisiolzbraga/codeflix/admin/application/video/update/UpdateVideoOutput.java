package com.tarcisiolzbraga.codeflix.admin.application.video.update;

import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;

public record UpdateVideoOutput(String id) {

    public static UpdateVideoOutput from(final Video video) {
        return new UpdateVideoOutput(video.getId().getValue());
    }
}
