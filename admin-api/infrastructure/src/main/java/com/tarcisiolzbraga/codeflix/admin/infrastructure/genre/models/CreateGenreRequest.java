package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models;

import java.util.Set;

public record CreateGenreRequest(String name, Set<String> categories, Boolean active) {

    // Sem categorias no JSON fica null: o CreateGenreCommand é quem troca por conjunto vazio.
    public CreateGenreRequest {
        categories = categories == null ? null : Set.copyOf(categories);
    }

    // Ausente no JSON significa ativo; o domínio é quem valida o resto.
    public boolean isActive() {
        return this.active == null || this.active;
    }
}
