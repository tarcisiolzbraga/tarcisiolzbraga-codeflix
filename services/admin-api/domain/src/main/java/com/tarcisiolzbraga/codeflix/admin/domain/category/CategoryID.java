package com.tarcisiolzbraga.codeflix.admin.domain.category;

import com.tarcisiolzbraga.codeflix.admin.domain.Identifier;
import java.util.Objects;
import java.util.UUID;

// O id é UUID, e não String, porque é o que ele sempre foi na prática: unique() sorteia um UUID e a
// coluna é CHAR(36). Com String, duas grafias do mesmo id eram dois ids diferentes — e como o MySQL
// compara ignorando a caixa, o banco achava a linha enquanto a comparação em memória dizia que não.
public record CategoryID(UUID value) implements Identifier {

    public CategoryID {
        Objects.requireNonNull(value, "'value' should not be null");
    }

    public static CategoryID unique() {
        return from(UUID.randomUUID());
    }

    public static CategoryID from(final String value) {
        return from(Identifier.uuidOf(value, CategoryID.class));
    }

    public static CategoryID from(final UUID value) {
        return new CategoryID(value);
    }

}
