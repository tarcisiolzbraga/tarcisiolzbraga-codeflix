package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import com.tarcisiolzbraga.codeflix.admin.application.video.update.UpdateVideoOutput;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateVideoResponse(@Schema(description = "Identificador do vídeo atualizado") String id) {

    public static UpdateVideoResponse from(final UpdateVideoOutput output) {
        return new UpdateVideoResponse(output.id());
    }
}
