package com.tarcisiolzbraga.codeflix.admin.application.video;

import com.tarcisiolzbraga.codeflix.admin.domain.video.AudioVideoMedia;

// A mídia de áudio e vídeo como sai da aplicação: o status vira texto na borda, como a
// classificação indicativa.
public record VideoMediaOutput(
        String checksum, String name, String rawLocation, String encodedLocation, String status) {

    public static VideoMediaOutput from(final AudioVideoMedia media) {
        return new VideoMediaOutput(
                media.checksum(), media.name(), media.rawLocation(), media.encodedLocation(), media.status().name());
    }
}
