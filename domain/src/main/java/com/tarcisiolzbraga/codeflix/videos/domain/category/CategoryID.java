package com.tarcisiolzbraga.codeflix.videos.domain.category;

import com.tarcisiolzbraga.codeflix.videos.domain.Identifier;
import java.util.Objects;

// Sem unique(): o id da categoria é gerado pelo admin-codeflix e chega junto com o dado. Aqui ele
// só é recebido, então não há fábrica que invente um.
public record CategoryID(String value) implements Identifier {

    public CategoryID {
        Objects.requireNonNull(value, "'value' should not be null");
    }

    public static CategoryID from(final String value) {
        return new CategoryID(value);
    }

    @Override
    public String getValue() {
        return this.value;
    }
}
