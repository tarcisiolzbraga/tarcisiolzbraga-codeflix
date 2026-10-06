package com.tarcisiolzbraga.codeflix.videos.application.video.save;

import java.time.Instant;

// Agrupado pelos mesmos quatro eixos do domínio. Achatado seriam dezenove campos num construtor, o
// que estoura o limite de dez do projeto — é a mesma razão que levou o Video a ter value objects.
public record SaveVideoCommand(
        String id,
        VideoDetailsCommand details,
        VideoFlagsCommand flags,
        VideoMediasCommand medias,
        VideoReferencesCommand references,
        Instant createdAt,
        Instant updatedAt) {
}
