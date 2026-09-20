package com.tarcisiolzbraga.codeflix.admin.application.category.update;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import io.vavr.control.Either;
import java.util.Objects;

public class DefaultUpdateCategoryUseCase extends UpdateCategoryUseCase {

    private final CategoryGateway categoryGateway;

    public DefaultUpdateCategoryUseCase(final CategoryGateway categoryGateway) {
        this.categoryGateway = Objects.requireNonNull(categoryGateway, "'categoryGateway' should not be null");
    }

    @Override
    public Either<Notification, UpdateCategoryOutput> execute(final UpdateCategoryCommand input) {
        final var category = findById(CategoryID.from(input.id()));
        final var notification = Notification.create();
        category.update(input.name(), input.description()).validate(notification);

        return notification.hasError() ? Left(notification) : update(category);
    }

    private Category findById(final CategoryID id) {
        return this.categoryGateway
                .findById(id)
                .orElseThrow(() -> NotFoundException.with(Category.class, id));
    }

    private Either<Notification, UpdateCategoryOutput> update(final Category category) {
        return Right(UpdateCategoryOutput.from(this.categoryGateway.update(category)));
    }
}
