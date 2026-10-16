package com.tarcisiolzbraga.codeflix.admin.domain.video;

// Os cinco arquivos do vídeo, só para a reconstrução vinda do banco, como o VideoFlags: quem envia
// um arquivo usa os métodos de intenção do Video. Componente nulo é mídia ainda não enviada, e é o
// Video que expõe isso como Optional — aqui um Optional viraria campo, o que o projeto não usa.
public record VideoMedias(
        AudioVideoMedia video,
        AudioVideoMedia trailer,
        ImageMedia banner,
        ImageMedia thumbnail,
        ImageMedia thumbnailHalf) {

    public static VideoMedias with(
            final AudioVideoMedia video,
            final AudioVideoMedia trailer,
            final ImageMedia banner,
            final ImageMedia thumbnail,
            final ImageMedia thumbnailHalf) {
        return new VideoMedias(video, trailer, banner, thumbnail, thumbnailHalf);
    }

    public static VideoMedias none() {
        return new VideoMedias(null, null, null, null, null);
    }
}
