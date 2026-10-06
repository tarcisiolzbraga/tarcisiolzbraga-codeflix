package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoMediaOutput;

// Arquivo ainda não enviado vira campo nulo no JSON: o campo continua no contrato, com valor nulo.
public record AudioVideoMediaResponse(
        String checksum, String name, String rawLocation, String encodedLocation, String status) {

    public static AudioVideoMediaResponse from(final VideoMediaOutput output) {
        return output == null
                ? null
                : new AudioVideoMediaResponse(
                        output.checksum(),
                        output.name(),
                        output.rawLocation(),
                        output.encodedLocation(),
                        output.status());
    }
}
