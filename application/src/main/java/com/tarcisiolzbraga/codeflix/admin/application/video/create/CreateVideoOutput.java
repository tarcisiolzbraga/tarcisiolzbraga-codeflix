package com.tarcisiolzbraga.codeflix.admin.application.video.create;

import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;

public record CreateVideoOutput(String id) {

    public static CreateVideoOutput from(final Video video) {
        return new CreateVideoOutput(video.getId().getValue());
    }
}
