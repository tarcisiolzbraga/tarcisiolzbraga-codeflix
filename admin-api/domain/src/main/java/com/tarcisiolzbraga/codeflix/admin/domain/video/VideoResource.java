package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.util.Objects;

// O arquivo mais o papel que ele cumpre no vídeo. Andam juntos desde a rota até o armazenamento:
// é o tipo que decide o endereço do arquivo e qual mídia do agregado será preenchida.
public record VideoResource(VideoMediaType type, Resource resource) {

    public VideoResource {
        Objects.requireNonNull(type, "'type' should not be null");
        Objects.requireNonNull(resource, "'resource' should not be null");
    }

    public static VideoResource with(final VideoMediaType type, final Resource resource) {
        return new VideoResource(type, resource);
    }
}
