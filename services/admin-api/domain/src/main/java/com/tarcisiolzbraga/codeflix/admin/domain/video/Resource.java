package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.util.Objects;

// O arquivo em si, a caminho do armazenamento.
//
// O conteúdo é um `MediaSource`: uma fonte que pode ser aberta mais de uma vez, e não um `byte[]`. O
// envio de 100 MB chegava a três cópias do arquivo inteiro em memória — `getBytes()` na rota, e as
// duas cópias defensivas deste record. Agora o arquivo é lido do temporário que o Spring já gravou
// em disco: uma passada para o checksum, outra para o PUT.
//
// **Sem cópia defensiva e sem `equals` por bytes**, e isso é consequência: não há arranjo a proteger,
// e comparar duas fontes exigiria abrir e ler as duas. Quem afirma conteúdo num teste abre a fonte e
// compara o que leu.
public record Resource(MediaSource content, String checksum, String contentType, String name) {

    public Resource {
        Objects.requireNonNull(content, "'content' should not be null");
        Objects.requireNonNull(checksum, "'checksum' should not be null");
        Objects.requireNonNull(contentType, "'contentType' should not be null");
        Objects.requireNonNull(name, "'name' should not be null");
    }

    public static Resource with(
            final MediaSource content, final String checksum, final String contentType, final String name) {
        return new Resource(content, checksum, contentType, name);
    }

    // A conveniência para conteúdo que já está na memória. Os testes a usam, e o download pequeno
    // também — ele é buffered de propósito, porque serve as artes do catálogo.
    public static Resource with(
            final byte[] content, final String checksum, final String contentType, final String name) {
        return new Resource(MediaSource.of(content), checksum, contentType, name);
    }
}
