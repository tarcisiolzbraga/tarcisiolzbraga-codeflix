package com.tarcisiolzbraga.codeflix.admin.application.video.list;

import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoPreview;
import java.time.Instant;

// A listagem não traz as referências: quem quer os ids busca o vídeo pelo id.
//
// O id sai como texto porque esta saída é o contrato do JSON, e é na borda que o UUID vira a
// forma canônica em texto.
public record VideoListOutput(
        String id, String title, Integer launchedAt, boolean published, boolean active, Instant createdAt) {

    public static VideoListOutput from(final VideoPreview preview) {
        return new VideoListOutput(
                preview.id().toString(),
                preview.title(),
                preview.launchedAt(),
                preview.published(),
                preview.active(),
                preview.createdAt());
    }
}
