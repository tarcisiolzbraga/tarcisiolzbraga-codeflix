package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.util.Objects;

// A mídia de imagem guardada. Não passa por codificação, então não tem status nem segundo endereço.
public record ImageMedia(String checksum, String name, String location) {

    public ImageMedia {
        Objects.requireNonNull(checksum, "'checksum' should not be null");
        Objects.requireNonNull(name, "'name' should not be null");
        Objects.requireNonNull(location, "'location' should not be null");
    }

    public static ImageMedia with(final String checksum, final String name, final String location) {
        return new ImageMedia(checksum, name, location);
    }
}
