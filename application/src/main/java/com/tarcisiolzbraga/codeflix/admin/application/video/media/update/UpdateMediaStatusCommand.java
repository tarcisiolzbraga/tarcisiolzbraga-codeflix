package com.tarcisiolzbraga.codeflix.admin.application.video.media.update;

import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaStatus;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;

// O vídeo mais o tipo dizem qual mídia mover; o caminho só interessa quando a codificação terminou.
public record UpdateMediaStatusCommand(
        String videoId, VideoMediaType type, MediaStatus status, String encodedPath) {
}
