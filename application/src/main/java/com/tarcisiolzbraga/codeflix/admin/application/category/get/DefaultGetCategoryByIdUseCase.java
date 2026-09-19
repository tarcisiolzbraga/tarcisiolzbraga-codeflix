package com.tarcisiolzbraga.codeflix.admin.application.category.get;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import java.util.Objects;

public class DefaultGetCategoryByIdUseCase extends GetCategoryByIdUseCase {

    private final CategoryGateway categoryGateway;

    public DefaultGetCategoryByIdUseCase(final CategoryGateway categoryGateway) {
        this.categoryGateway = Objects.requireNonNull(categoryGateway, "'categoryGateway' should not be null");
    }

    @Override
    public CategoryOutput execute(final String input) {
        final var id = CategoryID.from(input);
        return this.categoryGateway
                .findById(id)
                .map(CategoryOutput::from)
                .orElseThrow(() -> NotFoundException.with(Category.class, id));
    }
}
