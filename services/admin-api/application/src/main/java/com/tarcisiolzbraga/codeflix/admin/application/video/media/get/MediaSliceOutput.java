package com.tarcisiolzbraga.codeflix.admin.application.video.media.get;

import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaContent;
import com.tarcisiolzbraga.codeflix.admin.domain.video.ResourceSlice;

// O trecho que a rota devolve, com o `Content-Range` que o armazenamento calculou — é ele que
// permite responder 206 sem a rota refazer a conta do intervalo.
//
// O conteúdo é um `MediaContent`: ele atravessa este record sem ser lido, e é escrito direto na
// resposta. A versão anterior copiava o trecho duas vezes só aqui, por cópia defensiva no construtor
// e no accessor.
public record MediaSliceOutput(MediaContent content, String contentType, String name, String contentRange) {

    public static MediaSliceOutput from(final ResourceSlice slice) {
        return new MediaSliceOutput(slice.content(), slice.contentType(), slice.name(), slice.contentRange());
    }
}
