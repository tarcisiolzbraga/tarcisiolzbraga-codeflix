package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models;

import java.util.Set;

// A ativação não passa por aqui: ela tem rotas próprias.
public record UpdateGenreRequest(String name, Set<String> categories) {

    // Sem categorias no JSON fica null: o UpdateGenreCommand é quem troca por conjunto vazio.
    public UpdateGenreRequest {
        categories = categories == null ? null : Set.copyOf(categories);
    }
}
