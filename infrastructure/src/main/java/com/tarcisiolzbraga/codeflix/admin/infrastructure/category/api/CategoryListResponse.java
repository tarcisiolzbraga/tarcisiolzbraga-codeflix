package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.api;

import com.tarcisiolzbraga.codeflix.admin.application.category.list.CategoryListOutput;
import java.time.Instant;

public record CategoryListResponse(
        String id, String name, String description, boolean active, Instant createdAt, Instant deletedAt) {

    public static CategoryListResponse from(final CategoryListOutput output) {
        return new CategoryListResponse(
                output.id(),
                output.name(),
                output.description(),
                output.isActive(),
                output.createdAt(),
                output.deletedAt());
    }
}
