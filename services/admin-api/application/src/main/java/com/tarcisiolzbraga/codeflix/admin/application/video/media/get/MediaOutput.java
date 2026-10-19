package com.tarcisiolzbraga.codeflix.admin.application.video.media.get;

import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaContent;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Resource;

// O que a rota precisa para devolver o arquivo inteiro.
//
// O conteúdo é um `MediaContent` — uma função que escreve —, como o do trecho. A diferença é de onde
// ele vem: aqui a fonte é o `Resource` que o armazenamento devolveu, e **esse caminho continua
// montando o corpo em memória**, de propósito. Ele serve as artes do catálogo (banner e miniatura),
// que são pequenas, e o player sempre manda `Range`.
public record MediaOutput(MediaContent content, long length, String contentType, String name) {

    public static MediaOutput from(final Resource resource) {
        return new MediaOutput(
                destination -> {
                    try (var content = resource.content().open()) {
                        content.transferTo(destination);
                    }
                },
                resource.content().length(),
                resource.contentType(),
                resource.name());
    }
}
