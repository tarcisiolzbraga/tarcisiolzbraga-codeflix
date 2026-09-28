package com.tarcisiolzbraga.codeflix.admin.application.genre.delete;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import java.util.Objects;

public class DefaultDeleteGenreUseCase extends DeleteGenreUseCase {

    private final GenreGateway genreGateway;
    private final VideoGateway videoGateway;

    public DefaultDeleteGenreUseCase(final GenreGateway genreGateway, final VideoGateway videoGateway) {
        this.genreGateway = Objects.requireNonNull(genreGateway, "'genreGateway' should not be null");
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
    }

    // Agregado em uso não é removido, só desativado; a foreign key sem cascata segura o que escapar daqui.
    @Override
    public void execute(final String input) {
        final var id = GenreID.from(input);
        if (this.videoGateway.existsByGenre(id)) {
            throw ConflictException.linked(Genre.class, id, Video.class);
        }
        this.genreGateway.deleteById(id);
    }
}
