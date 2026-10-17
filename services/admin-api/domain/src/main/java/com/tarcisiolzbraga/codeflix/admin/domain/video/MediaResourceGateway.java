package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.util.Optional;

// O arquivo em si não mora no banco: este gateway fala com o armazenamento, enquanto o VideoGateway
// cuida do agregado. Guardar devolve a mídia já com o endereço, que é o que o vídeo passa a apontar.
public interface MediaResourceGateway {

    AudioVideoMedia storeAudioVideo(VideoID id, VideoResource resource);

    ImageMedia storeImage(VideoID id, VideoResource resource);

    Optional<Resource> getResource(VideoID id, VideoMediaType type);

    // Um trecho do arquivo, para o player poder buscar na linha de tempo sem baixar tudo.
    //
    // O `range` vai como o cliente o mandou, no formato do cabeçalho HTTP (`bytes=0-1023`): o
    // armazenamento fala esse formato nativamente, então interpretá-lo aqui seria repetir uma conta
    // que o S3 já faz — e errar nas pontas.
    Optional<ResourceSlice> getResourceSlice(VideoID id, VideoMediaType type, String range);

    // Apagar o vídeo apaga os arquivos dele: não há quem os referencie depois.
    void clearResources(VideoID id);
}
