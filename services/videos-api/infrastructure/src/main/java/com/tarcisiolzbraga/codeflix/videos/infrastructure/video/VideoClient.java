package com.tarcisiolzbraga.codeflix.videos.infrastructure.video;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.VideoDTO;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.VideoMediaFile;
import java.util.Optional;

public interface VideoClient {

    // Vazio quando o admin responde 404: entre o evento e esta chamada o vídeo pode ter sido apagado
    // lá, com o evento de remoção a caminho.
    Optional<VideoDTO> videoOfId(String id);

    // O arquivo de mídia do vídeo. Vazio quando o admin não tem aquele tipo para aquele id — é o
    // caso de um vídeo cadastrado sem upload, que existe no catálogo e não tem o que reproduzir.
    Optional<VideoMediaFile> mediaOf(String id, String type);
}
