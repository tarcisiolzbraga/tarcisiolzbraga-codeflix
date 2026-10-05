package com.tarcisiolzbraga.codeflix.videos.application.genre.save;

import com.tarcisiolzbraga.codeflix.videos.domain.genre.Genre;

public record SaveGenreOutput(String id) {

    public static SaveGenreOutput from(final Genre genre) {
        return new SaveGenreOutput(genre.getId().getValue());
    }
}
