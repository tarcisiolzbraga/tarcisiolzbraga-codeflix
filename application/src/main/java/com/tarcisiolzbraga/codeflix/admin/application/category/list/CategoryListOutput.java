package com.tarcisiolzbraga.codeflix.admin.application.category.list;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import java.time.Instant;

public record CategoryListOutput(
        String id, String name, String description, boolean isActive, Instant createdAt) {

    public static CategoryListOutput from(final Category category) {
        return new CategoryListOutput(
                category.getId().getValue(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt());
    }
}
