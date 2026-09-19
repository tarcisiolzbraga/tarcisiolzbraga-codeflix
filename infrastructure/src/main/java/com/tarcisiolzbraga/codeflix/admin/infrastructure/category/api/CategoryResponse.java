package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.api;

import com.tarcisiolzbraga.codeflix.admin.application.category.get.CategoryOutput;
import java.time.Instant;

public record CategoryResponse(
        String id,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {

    public static CategoryResponse from(final CategoryOutput output) {
        return new CategoryResponse(
                output.id(),
                output.name(),
                output.description(),
                output.isActive(),
                output.createdAt(),
                output.updatedAt(),
                output.deletedAt());
    }
}
