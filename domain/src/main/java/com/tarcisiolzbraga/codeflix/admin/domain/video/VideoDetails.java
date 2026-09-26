package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.time.Year;

// A ficha do vídeo: o que o usuário informa e o que o update substitui. Agrupado num value object
// para as fábricas e o update não passarem do limite de parâmetros.
public record VideoDetails(String title, String description, Year launchedAt, double duration, Rating rating) {

    public static VideoDetails with(
            final String title,
            final String description,
            final Year launchedAt,
            final double duration,
            final Rating rating) {
        return new VideoDetails(title, description, launchedAt, duration, rating);
    }
}
