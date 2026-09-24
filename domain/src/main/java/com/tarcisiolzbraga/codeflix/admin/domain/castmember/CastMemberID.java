package com.tarcisiolzbraga.codeflix.admin.domain.castmember;

import com.tarcisiolzbraga.codeflix.admin.domain.Identifier;
import java.util.Objects;
import java.util.UUID;

public record CastMemberID(String value) implements Identifier {

    public CastMemberID {
        Objects.requireNonNull(value, "'value' should not be null");
    }

    public static CastMemberID unique() {
        return from(UUID.randomUUID());
    }

    public static CastMemberID from(final String value) {
        return new CastMemberID(value);
    }

    public static CastMemberID from(final UUID value) {
        return new CastMemberID(value.toString().toLowerCase());
    }

    @Override
    public String getValue() {
        return this.value;
    }
}
