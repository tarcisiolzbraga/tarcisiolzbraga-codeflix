package com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

// O registro completo, como a API do admin-codeflix o devolve em GET /cast-members/{id}.
//
// O tipo vem como texto, que é como o admin o publica; convertê-lo para enum é trabalho do caso de
// uso, onde um valor desconhecido vira erro de validação junto dos outros da mesma mensagem.
@JsonIgnoreProperties(ignoreUnknown = true)
public record CastMemberDTO(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("active") boolean active,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("updatedAt") Instant updatedAt) {
}
