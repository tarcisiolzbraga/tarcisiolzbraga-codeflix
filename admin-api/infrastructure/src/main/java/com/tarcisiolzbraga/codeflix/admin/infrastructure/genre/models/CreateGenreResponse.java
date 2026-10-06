package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models;

import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreOutput;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateGenreResponse(@Schema(description = "Identificador do gênero criado") String id) {

    public static CreateGenreResponse from(final CreateGenreOutput output) {
        return new CreateGenreResponse(output.id());
    }
}
