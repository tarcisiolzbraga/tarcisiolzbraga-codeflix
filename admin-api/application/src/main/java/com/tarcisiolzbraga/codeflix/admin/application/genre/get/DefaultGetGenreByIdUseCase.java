package com.tarcisiolzbraga.codeflix.admin.application.genre.get;

import com.tarcisiolzbraga.codeflix.admin.application.genre.GenreOutput;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import java.util.Objects;

public class DefaultGetGenreByIdUseCase extends GetGenreByIdUseCase {

    private final GenreGateway genreGateway;

    public DefaultGetGenreByIdUseCase(final GenreGateway genreGateway) {
        this.genreGateway = Objects.requireNonNull(genreGateway, "'genreGateway' should not be null");
    }

    @Override
    public GenreOutput execute(final String input) {
        final var id = GenreID.from(input);
        return this.genreGateway
                .findById(id)
                .map(GenreOutput::from)
                .orElseThrow(() -> NotFoundException.with(Genre.class, id));
    }
}
