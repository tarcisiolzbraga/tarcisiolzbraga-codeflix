package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models;

import com.tarcisiolzbraga.codeflix.admin.application.category.CategoryOutput;
import java.time.Instant;

public record CategoryResponse(
        String id,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static CategoryResponse from(final CategoryOutput output) {
        return new CategoryResponse(
                output.id(),
                output.name(),
                output.description(),
                output.isActive(),
                output.createdAt(),
                output.updatedAt());
    }
}
