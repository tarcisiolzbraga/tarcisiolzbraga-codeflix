package com.tarcisiolzbraga.codeflix.videos.application.genre.save;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.handler.Notification;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultSaveGenreUseCase extends SaveGenreUseCase {

    private static final ValidationError NULL_ID = new ValidationError("'id' should not be null");

    private final GenreGateway genreGateway;

    public DefaultSaveGenreUseCase(final GenreGateway genreGateway) {
        this.genreGateway = Objects.requireNonNull(genreGateway, "'genreGateway' should not be null");
    }

    @Override
    public SaveGenreOutput execute(final SaveGenreCommand input) {
        Objects.requireNonNull(input, "'input' should not be null");
        if (input.id() == null) {
            throw DomainException.with(NULL_ID);
        }

        final var genre = toGenre(input);
        final var notification = Notification.create();
        genre.validate(notification);
        if (notification.hasError()) {
            throw DomainException.with(notification.getErrors());
        }

        return SaveGenreOutput.from(this.genreGateway.save(genre));
    }

    // Categorias nulas na mensagem entram como conjunto vazio, e não como erro: gênero sem categoria
    // é válido no admin-codeflix, e derrubar o consumo por isso seria pior que replicar sem vínculo —
    // o vínculo volta no próximo evento, que o refreshUpdatedAt de lá garante.
    private static Genre toGenre(final SaveGenreCommand input) {
        return Genre.with(
                GenreID.from(input.id()),
                input.name(),
                input.active(),
                categoriesOf(input.categories()),
                input.createdAt(),
                input.updatedAt());
    }

    private static Set<CategoryID> categoriesOf(final Set<String> categories) {
        if (categories == null) {
            return Set.of();
        }
        return categories.stream().filter(Objects::nonNull).map(CategoryID::from).collect(Collectors.toUnmodifiableSet());
    }
}
