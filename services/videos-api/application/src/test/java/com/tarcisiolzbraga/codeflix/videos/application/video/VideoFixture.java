package com.tarcisiolzbraga.codeflix.videos.application.video;

import com.tarcisiolzbraga.codeflix.videos.application.video.save.SaveVideoCommand;
import com.tarcisiolzbraga.codeflix.videos.application.video.save.VideoDetailsCommand;
import com.tarcisiolzbraga.codeflix.videos.application.video.save.VideoFlagsCommand;
import com.tarcisiolzbraga.codeflix.videos.application.video.save.VideoMediasCommand;
import com.tarcisiolzbraga.codeflix.videos.application.video.save.VideoReferencesCommand;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Video;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoDetails;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoFlags;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoMedias;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoReferences;
import java.time.Instant;
import java.time.Year;
import java.util.Set;

// Montagem comum dos testes do vídeo. Existe porque o agregado tem quatro value objects, e repetir
// a montagem em cada teste esconderia o que cada um deles está de fato verificando.
public final class VideoFixture {

    public static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    public static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    private VideoFixture() {
    }

    public static Video aVideo(final String id, final String title, final boolean published) {
        return Video.with(
                VideoID.from(id),
                VideoDetails.with(title, "Paul Atreides em Arrakis", Year.of(2026), 155.0, Rating.AGE_14),
                new VideoFlags(false, published, true),
                VideoMedias.with("v.mp4", "t.mp4", "b.jpg", "th.jpg", "thh.jpg"),
                VideoReferences.none(),
                CREATED_AT,
                UPDATED_AT);
    }

    public static SaveVideoCommand aCommand(final String id, final String title, final String rating) {
        return new SaveVideoCommand(
                id,
                new VideoDetailsCommand(title, "Paul Atreides em Arrakis", 2026, 155.0, rating),
                new VideoFlagsCommand(false, true, true),
                new VideoMediasCommand("v.mp4", "t.mp4", "b.jpg", "th.jpg", "thh.jpg"),
                new VideoReferencesCommand(Set.of("00000009-0000-0000-0000-000000000000"), Set.of("00000016-0000-0000-0000-000000000000"), Set.of("00000021-0000-0000-0000-000000000000")),
                CREATED_AT,
                UPDATED_AT);
    }
}
