package com.tarcisiolzbraga.codeflix.admin.application.genre.create;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

import com.tarcisiolzbraga.codeflix.admin.application.category.CategoryExistenceValidator;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import io.vavr.control.Either;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultCreateGenreUseCase extends CreateGenreUseCase {

    private final GenreGateway genreGateway;
    private final CategoryExistenceValidator categoryExistence;

    public DefaultCreateGenreUseCase(final CategoryGateway categoryGateway, final GenreGateway genreGateway) {
        this.genreGateway = Objects.requireNonNull(genreGateway, "'genreGateway' should not be null");
        this.categoryExistence = new CategoryExistenceValidator(categoryGateway);
    }

    @Override
    public Either<Notification, CreateGenreOutput> execute(final CreateGenreCommand input) {
        final var categories = toCategoryIds(input.categories());
        final var genre = Genre.newGenre(input.name(), input.isActive(), categories);
        final var notification = Notification.create();
        this.categoryExistence.validate(categories, notification);
        genre.validate(notification);

        if (notification.hasError()) {
            return Left(notification);
        }
        return Right(CreateGenreOutput.from(this.genreGateway.create(genre)));
    }

    private Set<CategoryID> toCategoryIds(final Set<String> ids) {
        return ids.stream().map(CategoryID::from).collect(Collectors.toUnmodifiableSet());
    }
}
