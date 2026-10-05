package com.tarcisiolzbraga.codeflix.videos.domain.castmember;

import com.tarcisiolzbraga.codeflix.videos.domain.Identifier;
import java.util.Objects;

// Sem unique(), como o CategoryID: o id é gerado pelo admin-codeflix e chega junto com o dado.
public record CastMemberID(String value) implements Identifier {

    public CastMemberID {
        Objects.requireNonNull(value, "'value' should not be null");
    }

    public static CastMemberID from(final String value) {
        return new CastMemberID(value);
    }

    @Override
    public String getValue() {
        return this.value;
    }
}
