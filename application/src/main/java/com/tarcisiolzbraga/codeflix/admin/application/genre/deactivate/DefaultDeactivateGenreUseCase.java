package com.tarcisiolzbraga.codeflix.admin.application.genre.deactivate;

import com.tarcisiolzbraga.codeflix.admin.application.genre.GenreOutput;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import java.util.Objects;

public class DefaultDeactivateGenreUseCase extends DeactivateGenreUseCase {

    private final GenreGateway genreGateway;

    public DefaultDeactivateGenreUseCase(final GenreGateway genreGateway) {
        this.genreGateway = Objects.requireNonNull(genreGateway, "'genreGateway' should not be null");
    }

    @Override
    public GenreOutput execute(final String input) {
        final var id = GenreID.from(input);
        final var genre = this.genreGateway
                .findById(id)
                .orElseThrow(() -> NotFoundException.with(Genre.class, id));
        genre.deactivate();
        return GenreOutput.from(this.genreGateway.update(genre));
    }
}
