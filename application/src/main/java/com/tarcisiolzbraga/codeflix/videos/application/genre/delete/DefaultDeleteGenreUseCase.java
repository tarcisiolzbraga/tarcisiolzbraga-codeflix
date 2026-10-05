package com.tarcisiolzbraga.codeflix.videos.application.genre.delete;

import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import java.util.Objects;

// Idempotente, como os outros: a remoção chega por mensagem, que pode ser reentregue. Id nulo é
// recusado em vez de ignorado — é mensagem corrompida.
public class DefaultDeleteGenreUseCase extends DeleteGenreUseCase {

    private final GenreGateway genreGateway;

    public DefaultDeleteGenreUseCase(final GenreGateway genreGateway) {
        this.genreGateway = Objects.requireNonNull(genreGateway, "'genreGateway' should not be null");
    }

    @Override
    public void execute(final GenreID input) {
        Objects.requireNonNull(input, "'input' should not be null");
        this.genreGateway.deleteById(input);
    }
}
