package com.tarcisiolzbraga.codeflix.videos.application.video.save;

// Os campos descritivos como vieram da mensagem: o ano como número e a classificação como texto, que
// é o caso de uso que converte.
public record VideoDetailsCommand(
        String title, String description, Integer launchedAt, double duration, String rating) {
}
