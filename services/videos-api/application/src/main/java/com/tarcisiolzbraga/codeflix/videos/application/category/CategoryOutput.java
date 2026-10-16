package com.tarcisiolzbraga.codeflix.videos.application.category;

import com.tarcisiolzbraga.codeflix.videos.domain.category.Category;
import java.time.Instant;

// Um único output serve às duas leituras da categoria, listar e buscar por ids, porque as duas
// alimentam a mesma projeção. Separá-los só duplicaria o mesmo conjunto de campos.
public record CategoryOutput(
        String id, String name, String description, boolean active, Instant createdAt, Instant updatedAt) {

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
