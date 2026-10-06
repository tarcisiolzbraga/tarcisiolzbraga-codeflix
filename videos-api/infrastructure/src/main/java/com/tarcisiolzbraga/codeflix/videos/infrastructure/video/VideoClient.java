package com.tarcisiolzbraga.codeflix.videos.infrastructure.video;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.VideoDTO;
import java.util.Optional;

public interface VideoClient {

    // Vazio quando o admin responde 404: entre o evento e esta chamada o vídeo pode ter sido apagado
    // lá, com o evento de remoção a caminho.
    Optional<VideoDTO> videoOfId(String id);
}
