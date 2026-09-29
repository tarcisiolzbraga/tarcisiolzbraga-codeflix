package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import com.tarcisiolzbraga.codeflix.admin.application.video.media.upload.UploadMediaOutput;

public record UploadMediaResponse(String videoId, String type) {

    public static UploadMediaResponse from(final UploadMediaOutput output) {
        return new UploadMediaResponse(output.videoId(), output.type().name());
    }
}
