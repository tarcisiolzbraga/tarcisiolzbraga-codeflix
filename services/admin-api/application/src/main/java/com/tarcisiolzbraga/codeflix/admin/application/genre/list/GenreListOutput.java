package com.tarcisiolzbraga.codeflix.admin.application.genre.list;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record GenreListOutput(
        String id, String name, boolean isActive, Set<String> categories, Instant createdAt) {

    public GenreListOutput {
        categories = Set.copyOf(categories);
    }

    public static GenreListOutput from(final Genre genre) {
        return new GenreListOutput(
                genre.getId().getValue(),
                genre.getName(),
                genre.isActive(),
                genre.getCategories().stream().map(CategoryID::getValue).collect(Collectors.toUnmodifiableSet()),
                genre.getCreatedAt());
    }
}
