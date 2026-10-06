package com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Da linha que vem no evento, só o id é lido. O resto do registro é buscado na API do
// admin-codeflix, porque o CDC não traz as tabelas de junção que completam um agregado — e assim
// renomear qualquer outra coluna lá não chega até aqui.
@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoryEvent(@JsonProperty("id") String id) {
}
