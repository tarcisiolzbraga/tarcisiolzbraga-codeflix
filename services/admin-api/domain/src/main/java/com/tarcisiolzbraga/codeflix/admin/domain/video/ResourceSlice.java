package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.util.Objects;

// Um trecho do arquivo, como o armazenamento o devolve quando se pede um Range.
//
// Existe separado do Resource porque carrega o que só faz sentido num trecho: **o `Content-Range`
// que o armazenamento calculou**, com o intervalo servido e o tamanho total. Recalculá-lo aqui seria
// repetir uma conta que o S3 já fez e já devolveu.
//
// O conteúdo é um `MediaContent`, e não um `byte[]`: o trecho é escrito direto na resposta, sem
// passar inteiro pela memória. Por isso **não há cópia defensiva nem `equals` por bytes** — não há
// bytes para copiar nem comparar, e um conteúdo só pode ser escrito uma vez. Quem quiser afirmar o
// conteúdo num teste escreve-o num destino e compara o que saiu, que é o que a rota faz.
public record ResourceSlice(MediaContent content, String contentType, String name, String contentRange) {

    public ResourceSlice {
        Objects.requireNonNull(content, "'content' should not be null");
        Objects.requireNonNull(contentType, "'contentType' should not be null");
        Objects.requireNonNull(name, "'name' should not be null");
        Objects.requireNonNull(contentRange, "'contentRange' should not be null");
    }
}
