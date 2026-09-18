package com.tarcisiolzbraga.codeflix.admin.application.category.create;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import java.util.Objects;

public class DefaultCreateCategoryUseCase extends CreateCategoryUseCase {

    private final CategoryGateway categoryGateway;

    public DefaultCreateCategoryUseCase(final CategoryGateway categoryGateway) {
        this.categoryGateway = Objects.requireNonNull(categoryGateway, "'categoryGateway' should not be null");
    }

    @Override
    public CreateCategoryOutput execute(final CreateCategoryCommand input) {
        final var category = Category.newCategory(input.name(), input.description(), input.isActive());
        validate(category);
        return CreateCategoryOutput.from(this.categoryGateway.create(category));
    }

    private void validate(final Category category) {
        final var notification = Notification.create();
        category.validate(notification);
        if (notification.hasError()) {
            throw DomainException.with(notification.getErrors());
        }
    }
}
