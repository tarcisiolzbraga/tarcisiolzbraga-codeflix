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

    // Value object: mudar de status é devolver outro, não alterar este.
    public AudioVideoMedia processing() {
        return new AudioVideoMedia(
                this.checksum, this.name, this.rawLocation, this.encodedLocation, MediaStatus.PROCESSING);
    }

    public AudioVideoMedia completed(final String encodedLocation) {
        return new AudioVideoMedia(
                this.checksum, this.name, this.rawLocation, encodedLocation, MediaStatus.COMPLETED);
    }

    // O `encodedLocation` é preservado em vez de limpo, e na prática ele está vazio: a falha chega
    // antes de haver saída. Preservá-lo é o que mantém este método honesto se um dia o codificador
    // falhar **depois** de escrever algo — aí o endereço parcial é informação, não lixo.
    public AudioVideoMedia failed() {
        return new AudioVideoMedia(
                this.checksum, this.name, this.rawLocation, this.encodedLocation, MediaStatus.FAILED);
    }
}
