package com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Da linha que vem no evento, só o id é lido — e aqui isso não é escolha, é necessidade: a linha do
// genre não tem as categorias, elas moram em genre_category, que o CDC não captura. Quem as traz é
// a API do admin-codeflix.
@JsonIgnoreProperties(ignoreUnknown = true)
public record GenreEvent(@JsonProperty("id") String id) {
}
