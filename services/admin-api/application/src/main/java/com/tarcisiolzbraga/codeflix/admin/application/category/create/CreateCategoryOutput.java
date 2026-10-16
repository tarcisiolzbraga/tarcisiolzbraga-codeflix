package com.tarcisiolzbraga.codeflix.admin.application.category.create;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;

public record CreateCategoryOutput(String id) {

    public static CreateCategoryOutput from(final Category category) {
        return new CreateCategoryOutput(category.getId().getValue());
    }
}
