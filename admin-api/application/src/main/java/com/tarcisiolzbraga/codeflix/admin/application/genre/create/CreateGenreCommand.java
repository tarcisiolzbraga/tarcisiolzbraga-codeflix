package com.tarcisiolzbraga.codeflix.admin.application.genre.create;

import java.util.Set;

public record CreateGenreCommand(String name, boolean isActive, Set<String> categories) {

    // A borda da aplicação aceita a ausência de categorias; o domínio segue exigindo o conjunto.
    public CreateGenreCommand {
        categories = categories == null ? Set.of() : Set.copyOf(categories);
    }

    public static CreateGenreCommand with(final String name, final boolean isActive, final Set<String> categories) {
        return new CreateGenreCommand(name, isActive, categories);
    }
}
