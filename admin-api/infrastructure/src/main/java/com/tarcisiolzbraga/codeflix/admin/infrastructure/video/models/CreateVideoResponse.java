package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import com.tarcisiolzbraga.codeflix.admin.application.video.create.CreateVideoOutput;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateVideoResponse(@Schema(description = "Identificador do vídeo criado") String id) {

    public static CreateVideoResponse from(final CreateVideoOutput output) {
        return new CreateVideoResponse(output.id());
    }
}
