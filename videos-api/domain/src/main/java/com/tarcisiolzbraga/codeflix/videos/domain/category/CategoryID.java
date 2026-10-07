package com.tarcisiolzbraga.codeflix.videos.domain.category;

import com.tarcisiolzbraga.codeflix.videos.domain.Identifier;
import java.util.Objects;
import java.util.UUID;

// Sem unique(): o id da categoria é gerado pelo admin-codeflix e chega junto com o dado. Aqui ele
// só é recebido, então não há fábrica que invente um — e por isso o from(String) é o único caminho
// de entrada, e é nele que o formato é conferido.
// Guarda UUID, e não String: duas grafias do mesmo id eram dois ids diferentes, e o id aqui
// atravessa três fronteiras — CDC, API do admin e Elasticsearch — em que a grafia podia divergir.
public record CategoryID(UUID value) implements Identifier {

    public CategoryID {
        Objects.requireNonNull(value, "'value' should not be null");
    }

    public static CategoryID from(final String value) {
        return new CategoryID(Identifier.uuidOf(value, CategoryID.class));
    }

    @Override
    public String getValue() {
        return this.value.toString();
    }
}
