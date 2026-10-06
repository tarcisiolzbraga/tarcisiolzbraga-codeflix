package com.tarcisiolzbraga.codeflix.admin.application.genre.update;

import java.util.Set;

// Sem isActive: ativar e desativar têm caso de uso próprio.
public record UpdateGenreCommand(String id, String name, Set<String> categories) {

    public UpdateGenreCommand {
        categories = categories == null ? Set.of() : Set.copyOf(categories);
    }

    public static UpdateGenreCommand with(final String id, final String name, final Set<String> categories) {
        return new UpdateGenreCommand(id, name, categories);
    }
}
