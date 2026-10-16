package com.tarcisiolzbraga.codeflix.admin.application.video;

import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;

// Os cinco arquivos na saída, agrupados para o VideoOutput não crescer em largura. Componente nulo
// é arquivo ainda não enviado, que na borda vira campo nulo no JSON.
public record VideoMediaOutputs(
        VideoMediaOutput video,
        VideoMediaOutput trailer,
        VideoImageOutput banner,
        VideoImageOutput thumbnail,
        VideoImageOutput thumbnailHalf) {

    public static VideoMediaOutputs from(final Video video) {
        return new VideoMediaOutputs(
                video.getVideo().map(VideoMediaOutput::from).orElse(null),
                video.getTrailer().map(VideoMediaOutput::from).orElse(null),
                video.getBanner().map(VideoImageOutput::from).orElse(null),
                video.getThumbnail().map(VideoImageOutput::from).orElse(null),
                video.getThumbnailHalf().map(VideoImageOutput::from).orElse(null));
    }
}
