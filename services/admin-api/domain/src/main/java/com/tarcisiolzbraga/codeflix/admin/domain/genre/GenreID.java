package com.tarcisiolzbraga.codeflix.admin.domain.genre;

import com.tarcisiolzbraga.codeflix.admin.domain.Identifier;
import java.util.Objects;
import java.util.UUID;

// O id é UUID, e não String, porque é o que ele sempre foi na prática: unique() sorteia um UUID e a
// coluna é CHAR(36). Com String, duas grafias do mesmo id eram dois ids diferentes — e como o MySQL
// compara ignorando a caixa, o banco achava a linha enquanto a comparação em memória dizia que não.
public record GenreID(UUID value) implements Identifier {

    public GenreID {
        Objects.requireNonNull(value, "'value' should not be null");
    }

    public static GenreID unique() {
        return from(UUID.randomUUID());
    }

    public static GenreID from(final String value) {
        return from(Identifier.uuidOf(value, GenreID.class));
    }

    public static GenreID from(final UUID value) {
        return new GenreID(value);
    }

}
