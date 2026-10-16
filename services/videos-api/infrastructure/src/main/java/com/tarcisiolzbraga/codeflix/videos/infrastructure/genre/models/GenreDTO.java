package com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Set;

// O registro completo, como a API do admin-codeflix o devolve em GET /genres/{id}.
//
// É aqui que as categorias chegam: o CDC não captura a tabela genre_category, então o evento só
// avisa que o gênero mudou e é esta resposta que diz a quais categorias ele está vinculado.
@JsonIgnoreProperties(ignoreUnknown = true)
public record GenreDTO(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("categories") Set<String> categories,
        @JsonProperty("active") boolean active,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("updatedAt") Instant updatedAt) {
}
