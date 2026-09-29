package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.util.Arrays;
import java.util.Optional;

// Os arquivos que um vídeo carrega: dois de áudio e vídeo, três de imagem.
public enum VideoMediaType {
    VIDEO,
    TRAILER,
    BANNER,
    THUMBNAIL,
    THUMBNAIL_HALF;

    // Converte sem lançar, como o Rating: tipo desconhecido na rota é 404, não uma exceção de conversão.
    public static Optional<VideoMediaType> of(final String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst();
    }
}
