package com.tarcisiolzbraga.codeflix.videos.domain.video;

import com.tarcisiolzbraga.codeflix.videos.domain.Identifier;
import java.util.Objects;

public record VideoID(String value) implements Identifier {

    public VideoID {
        Objects.requireNonNull(value, "'value' should not be null");
    }

    public static VideoID from(final String value) {
        return new VideoID(value);
    }

    @Override
    public String getValue() {
        return this.value;
    }
}
