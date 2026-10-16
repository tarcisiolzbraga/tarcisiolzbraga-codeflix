package com.tarcisiolzbraga.codeflix.videos.domain.video;

import java.time.Year;

// O que descreve o vídeo. Agrupado como no admin-codeflix: sem isto, a fábrica do Video teria
// dezoito parâmetros, como a do catálogo do curso.
public record VideoDetails(
        String title, String description, Year launchedAt, double duration, Rating rating) {

    public static VideoDetails with(
            final String title,
            final String description,
            final Year launchedAt,
            final double duration,
            final Rating rating) {
        return new VideoDetails(title, description, launchedAt, duration, rating);
    }
}
