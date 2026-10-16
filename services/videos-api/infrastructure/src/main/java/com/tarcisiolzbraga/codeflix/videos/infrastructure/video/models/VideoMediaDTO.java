package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Do áudio e vídeo que o admin publica, só o endereço do arquivo interessa ao catálogo: o checksum e
// o nome original são assunto de quem governa a mídia.
//
// Os nomes dos campos são os do AudioVideoMediaResponse do admin — rawLocation e encodedLocation.
@JsonIgnoreProperties(ignoreUnknown = true)
public record VideoMediaDTO(
        @JsonProperty("rawLocation") String rawLocation,
        @JsonProperty("encodedLocation") String encodedLocation,
        @JsonProperty("status") String status) {

    // O codificado quando já existe; antes disso, o cru, que é o que o admin tem para oferecer. O
    // catálogo não trata status: se há endereço, ele serve o endereço.
    public String address() {
        return this.encodedLocation == null || this.encodedLocation.isBlank()
                ? this.rawLocation
                : this.encodedLocation;
    }
}
