package com.tarcisiolzbraga.codeflix.admin.domain.category;

import com.tarcisiolzbraga.codeflix.admin.domain.Identifier;
import java.util.Objects;
import java.util.UUID;

public record CategoryID(String value) implements Identifier {

    public CategoryID {
        Objects.requireNonNull(value, "'value' should not be null");
    }

    public static CategoryID unique() {
        return from(UUID.randomUUID());
    }

    public static CategoryID from(final String value) {
        return new CategoryID(value);
    }

    public static CategoryID from(final UUID value) {
        return new CategoryID(value.toString().toLowerCase());
    }

    @Override
    public String getValue() {
        return this.value;
    }
}
