package com.tarcisiolzbraga.codeflix.admin.application.genre.create;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

import com.tarcisiolzbraga.codeflix.admin.application.ReferenceExistenceValidator;
import com.tarcisiolzbraga.codeflix.admin.application.ReferenceIds;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import io.vavr.control.Either;
import java.util.Objects;
import java.util.Set;

public class DefaultCreateGenreUseCase extends CreateGenreUseCase {

    private final GenreGateway genreGateway;
    private final ReferenceExistenceValidator<CategoryID> categoryExistence;

    public DefaultCreateGenreUseCase(final CategoryGateway categoryGateway, final GenreGateway genreGateway) {
        this.genreGateway = Objects.requireNonNull(genreGateway, "'genreGateway' should not be null");
        this.categoryExistence = new ReferenceExistenceValidator<>("categories", categoryGateway::findExistingIds);
    }

    @Override
    public Either<Notification, CreateGenreOutput> execute(final CreateGenreCommand input) {
        // A Notification nasce antes da conversão: id malformado é erro acumulado como os outros,
        // e não exceção que interrompe e esconde o resto do que está errado na requisição.
        final var notification = Notification.create();
        final var categories = ReferenceIds.parse(input.categories(), CategoryID::from, notification);
        final var genre = Genre.newGenre(input.name(), input.isActive(), categories);
        this.categoryExistence.validate(categories, notification);
        genre.validate(notification);

        if (notification.hasError()) {
            return Left(notification);
        }
        return Right(CreateGenreOutput.from(this.genreGateway.create(genre)));
    }

}
