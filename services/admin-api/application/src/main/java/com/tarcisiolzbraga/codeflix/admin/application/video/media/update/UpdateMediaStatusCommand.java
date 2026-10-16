package com.tarcisiolzbraga.codeflix.admin.application.video.media.update;

import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaStatus;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;

// O vídeo mais o tipo dizem qual compartimento, e o checksum diz qual envio: sem ele, a resposta
// de uma codificação antiga seria aplicada ao arquivo que a substituiu. O caminho só interessa
// quando a codificação terminou.
public record UpdateMediaStatusCommand(
        String videoId, VideoMediaType type, MediaStatus status, String checksum, String encodedPath) {
}
