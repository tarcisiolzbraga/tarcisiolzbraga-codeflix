package com.tarcisiolzbraga.codeflix.admin.domain.genre;

import com.tarcisiolzbraga.codeflix.admin.domain.Identifier;
import java.util.Objects;
import java.util.UUID;

public record GenreID(String value) implements Identifier {

    public GenreID {
        Objects.requireNonNull(value, "'value' should not be null");
    }

    public static GenreID unique() {
        return from(UUID.randomUUID());
    }

    public static GenreID from(final String value) {
        return new GenreID(value);
    }

    public static GenreID from(final UUID value) {
        return new GenreID(value.toString().toLowerCase());
    }

    @Override
    public String getValue() {
        return this.value;
    }
}
