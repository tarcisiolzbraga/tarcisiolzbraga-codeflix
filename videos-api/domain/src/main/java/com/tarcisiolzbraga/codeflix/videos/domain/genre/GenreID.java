package com.tarcisiolzbraga.codeflix.videos.domain.genre;

import com.tarcisiolzbraga.codeflix.videos.domain.Identifier;
import java.util.Objects;
import java.util.UUID;

// Sem unique(), como os outros IDs daqui: o id é gerado pelo admin-codeflix e chega com o dado.
// Guarda UUID, e não String: duas grafias do mesmo id eram dois ids diferentes, e o id aqui
// atravessa três fronteiras — CDC, API do admin e Elasticsearch — em que a grafia podia divergir.
public record GenreID(UUID value) implements Identifier {

    public GenreID {
        Objects.requireNonNull(value, "'value' should not be null");
    }

    public static GenreID from(final String value) {
        return new GenreID(Identifier.uuidOf(value, GenreID.class));
    }

    @Override
    public String getValue() {
        return this.value.toString();
    }
}
