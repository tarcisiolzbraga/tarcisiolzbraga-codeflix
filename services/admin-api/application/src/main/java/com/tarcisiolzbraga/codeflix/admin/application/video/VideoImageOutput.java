package com.tarcisiolzbraga.codeflix.admin.application.video;

import com.tarcisiolzbraga.codeflix.admin.domain.video.ImageMedia;

public record VideoImageOutput(String checksum, String name, String location) {

    public static VideoImageOutput from(final ImageMedia media) {
        return new VideoImageOutput(media.checksum(), media.name(), media.location());
    }
}
