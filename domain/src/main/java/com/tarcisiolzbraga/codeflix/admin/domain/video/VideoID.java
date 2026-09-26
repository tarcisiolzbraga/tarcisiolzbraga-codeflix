package com.tarcisiolzbraga.codeflix.admin.domain.video;

import com.tarcisiolzbraga.codeflix.admin.domain.Identifier;
import java.util.Objects;
import java.util.UUID;

public record VideoID(String value) implements Identifier {

    public VideoID {
        Objects.requireNonNull(value, "'value' should not be null");
    }

    public static VideoID unique() {
        return from(UUID.randomUUID());
    }

    public static VideoID from(final String value) {
        return new VideoID(value);
    }

    public static VideoID from(final UUID value) {
        return new VideoID(value.toString().toLowerCase());
    }

    @Override
    public String getValue() {
        return this.value;
    }
}
