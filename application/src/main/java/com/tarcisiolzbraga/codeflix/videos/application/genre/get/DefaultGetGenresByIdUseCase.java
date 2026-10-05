package com.tarcisiolzbraga.codeflix.videos.application.genre.get;

import com.tarcisiolzbraga.codeflix.videos.application.genre.GenreOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DefaultGetGenresByIdUseCase extends GetGenresByIdUseCase {

    private final GenreGateway genreGateway;

    public DefaultGetGenresByIdUseCase(final GenreGateway genreGateway) {
        this.genreGateway = Objects.requireNonNull(genreGateway, "'genreGateway' should not be null");
    }

    @Override
    public List<GenreOutput> execute(final Set<GenreID> input) {
        Objects.requireNonNull(input, "'input' should not be null");
        if (input.isEmpty()) {
            return List.of();
        }

        return this.genreGateway.findAllById(input).stream().map(GenreOutput::from).toList();
    }
}
