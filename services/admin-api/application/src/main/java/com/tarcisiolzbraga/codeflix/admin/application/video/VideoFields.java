package com.tarcisiolzbraga.codeflix.admin.application.video;

import com.tarcisiolzbraga.codeflix.admin.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoDetails;
import java.time.Year;

// Os dados do vídeo como chegam da borda: ano e classificação em tipos simples, convertidos aqui,
// para valor inválido virar erro de validação em vez de falha de desserialização.
public record VideoFields(String title, String description, Integer launchedAt, Double duration, String rating) {

    private static final double NO_DURATION = 0.0;

    // Rótulo ou ano desconhecido vira nulo, e quem reclama é o validador do domínio.
    public VideoDetails toDetails() {
        return VideoDetails.with(
                this.title,
                this.description,
                this.launchedAt == null ? null : Year.of(this.launchedAt),
                this.duration == null ? NO_DURATION : this.duration,
                Rating.of(this.rating).orElse(null));
    }
}
