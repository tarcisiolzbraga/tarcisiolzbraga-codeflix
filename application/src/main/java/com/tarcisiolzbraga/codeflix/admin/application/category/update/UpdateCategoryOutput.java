package com.tarcisiolzbraga.codeflix.admin.application.category.update;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;

public record UpdateCategoryOutput(String id) {

    public static UpdateCategoryOutput from(final Category category) {
        return new UpdateCategoryOutput(category.getId().getValue());
    }
}
