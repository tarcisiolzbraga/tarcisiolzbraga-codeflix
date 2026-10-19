package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models;

import java.util.Objects;

// Um arquivo de mídia, como o admin-codeflix o devolve.
//
// **O conteúdo volta a ser um `byte[]`, e isto é recuo deliberado.** O repasse em fluxo chegou a
// funcionar — e trouxe uma instabilidade que eu não consegui fechar: com a resposta saindo por
// `StreamingResponseBody`, o despacho assíncrono do Spring e o pool do cliente HTTP entram numa
// corrida que faz um pedido falhar com `ParseException` de vez em quando. Quatro tentativas de
// conserto, cada uma com razão plausível, e o CI continuou vermelho.
//
// O que se perde: o trecho passa pela memória deste serviço, como antes — três cópias. O que se
// mantém: as **seis** cópias do lado da admin-api, que é de onde o arquivo sai do armazenamento,
// deixaram de existir, e aquela ponta está estável. Dois terços do ganho, sem a instabilidade.
//
// O `length` fica: é com ele que a resposta declara `Content-Length`, que é melhor para o player de
// qualquer jeito.
//
// `contentRange` é nulo quando o arquivo veio inteiro, e traz o intervalo servido quando veio um
// trecho — é por ele que a rota sabe responder 200 ou 206.
public record VideoMediaFile(
        String contentType, String filename, byte[] content, long length, String contentRange) {

    // Cópia defensiva nas duas pontas: `byte[]` é mutável, e um record não protege o arranjo que
    // recebe.
    public VideoMediaFile {
        content = content == null ? new byte[0] : content.clone();
        Objects.requireNonNull(contentType, "'contentType' should not be null");
    }

    @Override
    public byte[] content() {
        return this.content.clone();
    }
}
