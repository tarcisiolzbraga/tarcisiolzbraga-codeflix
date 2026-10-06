package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Da linha que vem no evento, só o id é lido — e no vídeo isso é mais que escolha de desenho.
//
// Medido nas fixtures: a linha do video traz video_id, trailer_id, banner_id, thumbnail_id e
// thumbnail_half_id, que são chaves estrangeiras para as tabelas de mídia, nenhum endereço de
// arquivo; e nenhuma relação, que mora em video_category, video_genre e video_cast_member. Nada
// disso é capturado pelo CDC. Sem a chamada REST não haveria mídia nem relação.
@JsonIgnoreProperties(ignoreUnknown = true)
public record VideoEvent(@JsonProperty("id") String id) {
}
