package com.tarcisiolzbraga.codeflix.admin.application.video.media.get;

import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;

public record GetMediaCommand(String videoId, VideoMediaType type) {
}
