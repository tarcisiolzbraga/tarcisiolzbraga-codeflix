package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Imagem não passa por codificação, então tem um endereço só. O nome do campo é o do
// ImageMediaResponse do admin.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageMediaDTO(@JsonProperty("location") String location) {

    public String address() {
        return this.location;
    }
}
