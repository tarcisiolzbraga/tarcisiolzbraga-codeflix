package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoFields;
import com.tarcisiolzbraga.codeflix.admin.application.video.VideoReferenceIds;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

// Sem opened, published e active: cada um tem a sua rota.
public record UpdateVideoRequest(
        @Schema(description = "Título do vídeo") String title,
        @Schema(description = "Sinopse, até 4000 caracteres") String description,
        @Schema(description = "Ano de lançamento") Integer launchedAt,
        @Schema(description = "Duração em minutos") Double duration,
        @Schema(description = "Classificação indicativa: ER, L, 10, 12, 14, 16 ou 18") String rating,
        @Schema(description = "Ids das categorias") Set<String> categories,
        @Schema(description = "Ids dos gêneros") Set<String> genres,
        @Schema(description = "Ids dos membros de elenco") Set<String> castMembers) {

    public VideoFields toFields() {
        return new VideoFields(this.title, this.description, this.launchedAt, this.duration, this.rating);
    }

    public VideoReferenceIds toReferences() {
        return new VideoReferenceIds(this.categories, this.genres, this.castMembers);
    }
}
