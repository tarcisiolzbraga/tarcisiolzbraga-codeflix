package com.tarcisiolzbraga.codeflix.admin.application.video.media.upload;

import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoResource;

public record UploadMediaCommand(String videoId, VideoResource resource) {
}
