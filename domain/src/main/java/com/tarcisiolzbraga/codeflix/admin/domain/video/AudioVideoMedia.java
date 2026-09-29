package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.util.Objects;

// A mídia de áudio e vídeo já guardada: onde está o arquivo enviado, onde estará o codificado e em
// que ponto da codificação ela se encontra.
public record AudioVideoMedia(
        String checksum, String name, String rawLocation, String encodedLocation, MediaStatus status) {

    public AudioVideoMedia {
        Objects.requireNonNull(checksum, "'checksum' should not be null");
        Objects.requireNonNull(name, "'name' should not be null");
        Objects.requireNonNull(rawLocation, "'rawLocation' should not be null");
        Objects.requireNonNull(encodedLocation, "'encodedLocation' should not be null");
        Objects.requireNonNull(status, "'status' should not be null");
    }

    // Acabou de ser enviada: nada de codificado ainda.
    public static AudioVideoMedia with(final String checksum, final String name, final String rawLocation) {
        return new AudioVideoMedia(checksum, name, rawLocation, "", MediaStatus.PENDING);
    }

    public static AudioVideoMedia with(
            final String checksum,
            final String name,
            final String rawLocation,
            final String encodedLocation,
            final MediaStatus status) {
        return new AudioVideoMedia(checksum, name, rawLocation, encodedLocation, status);
    }
}
