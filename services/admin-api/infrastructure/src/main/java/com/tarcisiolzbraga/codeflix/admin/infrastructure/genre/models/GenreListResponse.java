package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models;

import com.tarcisiolzbraga.codeflix.admin.application.genre.list.GenreListOutput;
import java.time.Instant;
import java.util.List;

public record GenreListResponse(String id, String name, List<String> categories, boolean active, Instant createdAt) {

    public GenreListResponse {
        categories = List.copyOf(categories);
    }

    // Ordenadas para o JSON não depender da ordem de iteração do Set.
    public static GenreListResponse from(final GenreListOutput output) {
        return new GenreListResponse(
                output.id(),
                output.name(),
                output.categories().stream().sorted().toList(),
                output.isActive(),
                output.createdAt());
    }
}
