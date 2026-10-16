package com.tarcisiolzbraga.codeflix.admin.application.category.activate;

import com.tarcisiolzbraga.codeflix.admin.application.category.CategoryOutput;
import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import java.util.Objects;

public class DefaultActivateCategoryUseCase extends ActivateCategoryUseCase {

    private final CategoryGateway categoryGateway;

    public DefaultActivateCategoryUseCase(final CategoryGateway categoryGateway) {
        this.categoryGateway = Objects.requireNonNull(categoryGateway, "'categoryGateway' should not be null");
    }

    @Override
    public CategoryOutput execute(final String input) {
        final var id = CategoryID.from(input);
        final var category = this.categoryGateway
                .findById(id)
                .orElseThrow(() -> NotFoundException.with(Category.class, id));
        category.activate();
        return CategoryOutput.from(this.categoryGateway.update(category));
    }
}
