package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoImageOutput;

public record ImageMediaResponse(String checksum, String name, String location) {

    public static ImageMediaResponse from(final VideoImageOutput output) {
        return output == null ? null : new ImageMediaResponse(output.checksum(), output.name(), output.location());
    }
}
