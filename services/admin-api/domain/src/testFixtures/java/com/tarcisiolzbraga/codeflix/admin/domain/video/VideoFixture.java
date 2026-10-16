package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.time.Year;

// Dados de teste do vídeo, iguais em todo teste que não se importa com os valores. Sem sorteio: o
// teste que falha falha sempre, e o bloco given continua dizendo o que importa para aquele caso.
public final class VideoFixture {

    public static final String TITLE = "Duna";
    public static final String DESCRIPTION = "Paul Atreides em Arrakis";
    public static final Year LAUNCHED_AT = Year.of(2021);
    public static final double DURATION = 155.0;
    public static final Rating RATING = Rating.AGE_12;

    private VideoFixture() {
    }

    public static VideoDetails details() {
        return details(TITLE);
    }

    public static VideoDetails details(final String title) {
        return VideoDetails.with(title, DESCRIPTION, LAUNCHED_AT, DURATION, RATING);
    }

    public static Video video() {
        return Video.newVideo(details(), VideoReferences.none());
    }

    public static Video video(final String title) {
        return Video.newVideo(details(title), VideoReferences.none());
    }

    public static Video videoWith(final VideoReferences references) {
        return Video.newVideo(details(), references);
    }
}
