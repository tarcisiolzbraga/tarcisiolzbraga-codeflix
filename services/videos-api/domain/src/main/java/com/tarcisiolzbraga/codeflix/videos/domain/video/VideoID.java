package com.tarcisiolzbraga.codeflix.videos.domain.video;

import com.tarcisiolzbraga.codeflix.videos.domain.Identifier;
import java.util.Objects;
import java.util.UUID;

// Guarda UUID, e não String: duas grafias do mesmo id eram dois ids diferentes, e o id aqui
// atravessa três fronteiras — CDC, API do admin e Elasticsearch — em que a grafia podia divergir.
public record VideoID(UUID value) implements Identifier {

    public VideoID {
        Objects.requireNonNull(value, "'value' should not be null");
    }

    public static VideoID from(final String value) {
        return new VideoID(Identifier.uuidOf(value, VideoID.class));
    }

    // Para quem já tem o valor tipado e não precisa passar pelo texto: o documento do
    // Elasticsearch, que guarda os ids das relações como UUID.
    public static VideoID from(final UUID value) {
        return new VideoID(value);
    }

    @Override
    public String getValue() {
        return this.value.toString();
    }
}
