package com.tarcisiolzbraga.codeflix.admin.application.genre.update;

import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;

public record UpdateGenreOutput(String id) {

    public static UpdateGenreOutput from(final Genre genre) {
        return new UpdateGenreOutput(genre.getId().getValue());
    }
}
