package com.tarcisiolzbraga.codeflix.videos.application.genre;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.Genre;
import java.time.Instant;
import java.util.Set;

// As categorias saem como CategoryID, não como texto: o ID tipado é value object do domínio, e é
// exatamente o que a borda precisa para pedir as categorias ao GetCategoriesByIdUseCase.
public record GenreOutput(
        String id,
        String name,
        boolean active,
        Set<CategoryID> categories,
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
                genre.getCategories(),
                genre.getCreatedAt(),
                genre.getUpdatedAt());
    }
}
