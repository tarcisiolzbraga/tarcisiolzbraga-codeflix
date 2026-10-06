package com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

// O registro completo, como a API do admin-codeflix o devolve em GET /categories/{id}. É este
// contrato que importa de verdade: o evento do CDC só traz o id, e o resto vem daqui.
//
// ignoreUnknown porque o contrato é do admin: campo novo lá não deve derrubar a replicação aqui.
@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoryDTO(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("active") boolean active,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("updatedAt") Instant updatedAt) {
}
