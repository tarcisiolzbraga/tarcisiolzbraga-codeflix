package com.tarcisiolzbraga.codeflix.admin.application.genre.create;

import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;

public record CreateGenreOutput(String id) {

    public static CreateGenreOutput from(final Genre genre) {
        return new CreateGenreOutput(genre.getId().getValue());
    }
}
