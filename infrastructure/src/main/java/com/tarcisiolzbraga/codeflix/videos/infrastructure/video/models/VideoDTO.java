package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Set;

// O registro completo, como a API do admin-codeflix o devolve em GET /videos/{id}.
//
// Plano porque o contrato dele é plano: o VideoResponse de lá tem os dezenove campos no primeiro
// nível, com a nota "plano de propósito: é o contrato da API". Quem agrupa é o comando, depois.
//
// As mídias chegam como objeto no JSON do admin, com status e caminhos; aqui só o endereço do
// arquivo codificado interessa, e é ele que os records de mídia expõem.
@JsonIgnoreProperties(ignoreUnknown = true)
public record VideoDTO(
        @JsonProperty("id") String id,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("launchedAt") Integer launchedAt,
        @JsonProperty("duration") Double duration,
        @JsonProperty("rating") String rating,
        @JsonProperty("opened") boolean opened,
        @JsonProperty("published") boolean published,
        @JsonProperty("active") boolean active,
        @JsonProperty("categories") Set<String> categories,
        @JsonProperty("genres") Set<String> genres,
        @JsonProperty("castMembers") Set<String> castMembers,
        @JsonProperty("video") VideoMediaDTO video,
        @JsonProperty("trailer") VideoMediaDTO trailer,
        @JsonProperty("banner") ImageMediaDTO banner,
        @JsonProperty("thumbnail") ImageMediaDTO thumbnail,
        @JsonProperty("thumbnailHalf") ImageMediaDTO thumbnailHalf,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("updatedAt") Instant updatedAt) {
}
