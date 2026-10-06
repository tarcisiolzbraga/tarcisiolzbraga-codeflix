package com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Da linha que vem no evento, só o id é lido; o resto do registro é buscado na API do
// admin-codeflix. Assim renomear qualquer outra coluna lá não chega até aqui.
@JsonIgnoreProperties(ignoreUnknown = true)
public record CastMemberEvent(@JsonProperty("id") String id) {
}
