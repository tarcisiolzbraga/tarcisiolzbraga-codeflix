package com.tarcisiolzbraga.codeflix.admin.application.video.list;

import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import java.time.Instant;

// A listagem não traz as referências: quem quer os ids busca o vídeo pelo id.
public record VideoListOutput(
        String id, String title, Integer launchedAt, boolean published, boolean active, Instant createdAt) {

    public static VideoListOutput from(final Video video) {
        return new VideoListOutput(
                video.getId().getValue(),
                video.getTitle(),
                video.getLaunchedAt() == null ? null : video.getLaunchedAt().getValue(),
                video.isPublished(),
                video.isActive(),
                video.getCreatedAt());
    }
}
