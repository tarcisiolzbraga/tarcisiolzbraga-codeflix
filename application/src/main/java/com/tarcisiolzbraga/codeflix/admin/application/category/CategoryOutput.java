package com.tarcisiolzbraga.codeflix.admin.application.category;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import java.time.Instant;

public record CategoryOutput(
        String id,
        String name,
        String description,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt) {

    public static CategoryOutput from(final Category category) {
        return new CategoryOutput(
                category.getId().getValue(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
