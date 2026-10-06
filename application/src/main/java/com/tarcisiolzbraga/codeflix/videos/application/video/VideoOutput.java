package com.tarcisiolzbraga.codeflix.videos.application.video;

import com.tarcisiolzbraga.codeflix.videos.domain.video.Video;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoDetails;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoFlags;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoMedias;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoReferences;
import java.time.Instant;

// Reaproveita os value objects do domínio em vez de achatar dezessete campos: eles são value
// objects, não entidades, e é o mesmo caminho do CastMemberOutput, que carrega o enum, e do
// GenreOutput, que carrega os CategoryID.
public record VideoOutput(
        String id,
        VideoDetails details,
        VideoFlags flags,
        VideoMedias medias,
        VideoReferences references,
        Instant createdAt,
        Instant updatedAt) {

    public static VideoOutput from(final Video video) {
        return new VideoOutput(
                video.getId().getValue(),
                video.getDetails(),
                video.getFlags(),
                video.getMedias(),
                video.getReferences(),
                video.getCreatedAt(),
                video.getUpdatedAt());
    }
}
