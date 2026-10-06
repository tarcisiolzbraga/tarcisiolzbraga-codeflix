package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models;

import com.tarcisiolzbraga.codeflix.admin.application.genre.GenreOutput;
import java.time.Instant;
import java.util.List;

public record GenreResponse(
        String id,
        String name,
        List<String> categories,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public GenreResponse {
        categories = List.copyOf(categories);
    }

    // Ordenadas para o JSON não depender da ordem de iteração do Set.
    public static GenreResponse from(final GenreOutput output) {
        return new GenreResponse(
                output.id(),
                output.name(),
                output.categories().stream().sorted().toList(),
                output.isActive(),
                output.createdAt(),
                output.updatedAt());
    }
}
