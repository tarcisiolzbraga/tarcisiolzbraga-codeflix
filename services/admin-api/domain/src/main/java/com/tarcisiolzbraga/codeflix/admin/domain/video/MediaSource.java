package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.io.IOException;
import java.io.InputStream;

// De onde o conteúdo de um arquivo vem, **sem ele estar na memória**.
//
// Diferente do `MediaContent`, que escreve uma vez e acabou, esta fonte pode ser **aberta mais de uma
// vez** — e isso não é luxo, é o que o envio exige: o checksum é gravado como metadado do objeto, e
// portanto tem de ser conhecido **antes** do PUT. Uma passada calcula o checksum, outra envia.
//
// Duas passadas sobre o arquivo são de graça aqui: acima do limiar de memória, o Spring já grava o
// multipart num arquivo temporário em disco, e abrir o `getInputStream()` dele duas vezes é ler duas
// vezes um arquivo local. A alternativa — manter os bytes em memória para só precisar de uma passada —
// é justamente o custo que isto remove.
//
// O tamanho vem junto porque o PUT do S3 o exige: sem `Content-Length` o SDK bufferiza o corpo para
// descobri-lo, o que anularia tudo.
public interface MediaSource {

    InputStream open() throws IOException;

    long length();

    // Para conteúdo que **já** está na memória: a resposta de um download pequeno, e os arquivos dos
    // testes. Não finge ser fluxo — diz que é um arranjo, e o arranjo é a fonte.
    static MediaSource of(final byte[] content) {
        final var copy = content.clone();
        return new MediaSource() {
            @Override
            public InputStream open() {
                return new java.io.ByteArrayInputStream(copy);
            }

            @Override
            public long length() {
                return copy.length;
            }
        };
    }
}
