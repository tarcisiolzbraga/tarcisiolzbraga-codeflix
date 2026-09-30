package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// O que o codificador devolve, em três formas. O campo status diz qual delas é.
//
// As anotações vêm de com.fasterxml.jackson.annotation mesmo no Jackson 3: só core e databind
// mudaram para tools.jackson; o artefato de anotações manteve o pacote antigo.
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "status")
@JsonSubTypes({
    @JsonSubTypes.Type(value = VideoEncoderProcessing.class, name = "PROCESSING"),
    @JsonSubTypes.Type(value = VideoEncoderCompleted.class, name = "COMPLETED"),
    @JsonSubTypes.Type(value = VideoEncoderError.class, name = "ERROR")
})
public sealed interface VideoEncoderResult
        permits VideoEncoderProcessing, VideoEncoderCompleted, VideoEncoderError {

    String videoId();

    // Texto, e não VideoMediaType: tipo desconhecido é para o consumidor registrar e descartar,
    // não para derrubar a desserialização e a mensagem voltar para sempre.
    String type();

    // O mesmo checksum que saiu no aviso de envio: é ele que diz de qual envio esta resposta fala.
    String checksum();
}
