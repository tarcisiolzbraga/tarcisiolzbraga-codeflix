package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models;

import java.util.Objects;

// Um arquivo de mídia, como o admin-codeflix o devolve: o conteúdo, o tipo e o nome com que foi
// enviado.
//
// O conteúdo é um `MediaContent`, e não um `byte[]`: o arquivo atravessa o catálogo em fluxo, sem
// passar inteiro pela memória. A conexão com o admin fica **aberta** até o `writeTo` ser chamado, e
// é ele que a fecha.
//
// `contentRange` é nulo quando o arquivo veio inteiro, e traz o intervalo servido quando veio um
// trecho — é por ele que a rota sabe responder 200 ou 206, sem refazer a conta que o armazenamento
// já fez.
public record VideoMediaFile(
        String contentType, String filename, MediaContent content, String contentRange) {

    public VideoMediaFile {
        Objects.requireNonNull(content, "'content' should not be null");
    }
}
