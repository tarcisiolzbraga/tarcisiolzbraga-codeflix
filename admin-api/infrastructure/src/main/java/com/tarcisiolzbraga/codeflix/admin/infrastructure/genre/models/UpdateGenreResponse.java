package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models;

import com.tarcisiolzbraga.codeflix.admin.application.genre.update.UpdateGenreOutput;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateGenreResponse(@Schema(description = "Identificador do gênero atualizado") String id) {

    public static UpdateGenreResponse from(final UpdateGenreOutput output) {
        return new UpdateGenreResponse(output.id());
    }
}
