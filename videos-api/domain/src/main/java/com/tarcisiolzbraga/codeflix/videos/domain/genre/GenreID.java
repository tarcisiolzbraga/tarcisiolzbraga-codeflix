package com.tarcisiolzbraga.codeflix.videos.domain.genre;

import com.tarcisiolzbraga.codeflix.videos.domain.Identifier;
import java.util.Objects;

// Sem unique(), como os outros IDs daqui: o id é gerado pelo admin-codeflix e chega com o dado.
public record GenreID(String value) implements Identifier {

    public GenreID {
        Objects.requireNonNull(value, "'value' should not be null");
    }

    public static GenreID from(final String value) {
        return new GenreID(value);
    }

    @Override
    public String getValue() {
        return this.value;
    }
}
