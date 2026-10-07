package com.tarcisiolzbraga.codeflix.videos.domain.castmember;

import com.tarcisiolzbraga.codeflix.videos.domain.Identifier;
import java.util.Objects;
import java.util.UUID;

// Sem unique(), como o CategoryID: o id é gerado pelo admin-codeflix e chega junto com o dado.
// Guarda UUID, e não String: duas grafias do mesmo id eram dois ids diferentes, e o id aqui
// atravessa três fronteiras — CDC, API do admin e Elasticsearch — em que a grafia podia divergir.
public record CastMemberID(UUID value) implements Identifier {

    public CastMemberID {
        Objects.requireNonNull(value, "'value' should not be null");
    }

    public static CastMemberID from(final String value) {
        return new CastMemberID(Identifier.uuidOf(value, CastMemberID.class));
    }

    @Override
    public String getValue() {
        return this.value.toString();
    }
}
