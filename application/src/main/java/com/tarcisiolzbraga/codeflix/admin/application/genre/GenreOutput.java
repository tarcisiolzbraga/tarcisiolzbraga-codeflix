package com.tarcisiolzbraga.codeflix.admin.application.genre;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record GenreOutput(
        String id,
        String name,
        boolean isActive,
        Set<String> categories,
        Instant createdAt,
        Instant updatedAt) {

    public GenreOutput {
        categories = Set.copyOf(categories);
    }

    public static GenreOutput from(final Genre genre) {
        return new GenreOutput(
                genre.getId().getValue(),
                genre.getName(),
                genre.isActive(),
                genre.getCategories().stream().map(CategoryID::getValue).collect(Collectors.toUnmodifiableSet()),
                genre.getCreatedAt(),
                genre.getUpdatedAt());
    }
}
