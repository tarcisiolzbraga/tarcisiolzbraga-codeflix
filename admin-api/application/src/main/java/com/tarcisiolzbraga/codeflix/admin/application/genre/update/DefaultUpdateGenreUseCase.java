package com.tarcisiolzbraga.codeflix.admin.application.genre.update;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

import com.tarcisiolzbraga.codeflix.admin.application.ReferenceExistenceValidator;
import com.tarcisiolzbraga.codeflix.admin.application.ReferenceIds;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import io.vavr.control.Either;
import java.util.Objects;
import java.util.Set;

public class DefaultUpdateGenreUseCase extends UpdateGenreUseCase {

    private final GenreGateway genreGateway;
    private final ReferenceExistenceValidator<CategoryID> categoryExistence;

    public DefaultUpdateGenreUseCase(final CategoryGateway categoryGateway, final GenreGateway genreGateway) {
        this.genreGateway = Objects.requireNonNull(genreGateway, "'genreGateway' should not be null");
        this.categoryExistence = new ReferenceExistenceValidator<>("categories", categoryGateway::findExistingIds);
    }

    @Override
    public Either<Notification, UpdateGenreOutput> execute(final UpdateGenreCommand input) {
        final var genre = findById(GenreID.from(input.id()));
        // A Notification nasce antes da conversão: id malformado é erro acumulado como os outros,
        // e não exceção que interrompe e esconde o resto do que está errado na requisição.
        final var notification = Notification.create();
        final var categories = ReferenceIds.parse(input.categories(), CategoryID::from, notification);
        this.categoryExistence.validate(categories, notification);
        genre.update(input.name(), categories).validate(notification);

        return notification.hasError() ? Left(notification) : update(genre);
    }

    private Genre findById(final GenreID id) {
        return this.genreGateway
                .findById(id)
                .orElseThrow(() -> NotFoundException.with(Genre.class, id));
    }


    private Either<Notification, UpdateGenreOutput> update(final Genre genre) {
        return Right(UpdateGenreOutput.from(this.genreGateway.update(genre)));
    }
}
