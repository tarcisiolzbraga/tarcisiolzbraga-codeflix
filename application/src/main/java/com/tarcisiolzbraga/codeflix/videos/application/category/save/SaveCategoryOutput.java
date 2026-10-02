package com.tarcisiolzbraga.codeflix.videos.application.category.save;

import com.tarcisiolzbraga.codeflix.videos.domain.category.Category;

public record SaveCategoryOutput(String id) {

    public static SaveCategoryOutput from(final Category category) {
        return new SaveCategoryOutput(category.getId().getValue());
    }
}
