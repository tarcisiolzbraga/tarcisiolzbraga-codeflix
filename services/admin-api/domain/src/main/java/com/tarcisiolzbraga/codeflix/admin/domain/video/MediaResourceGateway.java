package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.util.Optional;

// O arquivo em si não mora no banco: este gateway fala com o armazenamento, enquanto o VideoGateway
// cuida do agregado. Guardar devolve a mídia já com o endereço, que é o que o vídeo passa a apontar.
public interface MediaResourceGateway {

    AudioVideoMedia storeAudioVideo(VideoID id, VideoResource resource);

    ImageMedia storeImage(VideoID id, VideoResource resource);

    Optional<Resource> getResource(VideoID id, VideoMediaType type);

    // Apagar o vídeo apaga os arquivos dele: não há quem os referencie depois.
    void clearResources(VideoID id);
}
