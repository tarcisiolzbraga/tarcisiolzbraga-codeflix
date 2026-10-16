package com.tarcisiolzbraga.codeflix.videos.application.video.save;

import com.tarcisiolzbraga.codeflix.videos.domain.video.Video;

public record SaveVideoOutput(String id) {

    public static SaveVideoOutput from(final Video video) {
        return new SaveVideoOutput(video.getId().getValue());
    }
}
