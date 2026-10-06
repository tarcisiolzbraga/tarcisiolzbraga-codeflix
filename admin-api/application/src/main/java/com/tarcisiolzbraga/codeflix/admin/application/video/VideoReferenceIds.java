package com.tarcisiolzbraga.codeflix.admin.application.video;

import java.util.Set;

public record VideoReferenceIds(Set<String> categories, Set<String> genres, Set<String> castMembers) {

    // A borda da aplicação aceita a ausência dos conjuntos; o domínio segue exigindo cada um deles.
    public VideoReferenceIds {
        categories = categories == null ? Set.of() : Set.copyOf(categories);
        genres = genres == null ? Set.of() : Set.copyOf(genres);
        castMembers = castMembers == null ? Set.of() : Set.copyOf(castMembers);
    }

    public static VideoReferenceIds none() {
        return new VideoReferenceIds(Set.of(), Set.of(), Set.of());
    }
}
