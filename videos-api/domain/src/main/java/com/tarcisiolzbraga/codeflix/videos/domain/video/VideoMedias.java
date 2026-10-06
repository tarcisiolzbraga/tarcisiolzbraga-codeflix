package com.tarcisiolzbraga.codeflix.videos.domain.video;

// Os endereços dos arquivos do vídeo.
//
// Só o endereço, ao contrário do admin-codeflix, onde VideoMedias guarda o AudioVideoMedia e o
// ImageMedia inteiros, com status de codificação, checksum e caminho cru. Nada disso serve a quem
// consome o catálogo: ele quer o arquivo pronto, e o resto é assunto de quem governa a mídia.
public record VideoMedias(
        String video, String trailer, String banner, String thumbnail, String thumbnailHalf) {

    public static VideoMedias with(
            final String video,
            final String trailer,
            final String banner,
            final String thumbnail,
            final String thumbnailHalf) {
        return new VideoMedias(video, trailer, banner, thumbnail, thumbnailHalf);
    }

    public static VideoMedias none() {
        return new VideoMedias(null, null, null, null, null);
    }
}
