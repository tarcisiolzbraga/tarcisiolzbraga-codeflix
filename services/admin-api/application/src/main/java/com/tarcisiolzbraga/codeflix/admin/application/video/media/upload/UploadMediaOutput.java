package com.tarcisiolzbraga.codeflix.admin.application.video.media.upload;

import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;

public record UploadMediaOutput(String videoId, VideoMediaType type) {
}
