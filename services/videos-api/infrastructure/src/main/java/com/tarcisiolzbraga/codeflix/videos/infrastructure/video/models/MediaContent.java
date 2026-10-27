package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models;

import java.io.IOException;
import java.io.OutputStream;

// O conteúdo de uma mídia que **ainda não foi lido**: quem o quer diz onde escrevê-lo.
//
// Repasse de verdade: o catálogo não guarda o arquivo, ele o entrega. Enquanto o conteúdo era um
// `byte[]`, "repassar" significava montar o trecho inteiro em memória aqui para depois escrevê-lo —
// três cópias só deste lado, somadas às seis da admin-api.
//
// A assinatura é a mesma do `StreamingResponseBody` do Spring, e a mesma do `MediaContent` da
// admin-api. Não é código compartilhado — os dois serviços têm builds independentes, e um módulo
// comum para uma interface de um método custaria mais que a repetição.
@FunctionalInterface
public interface MediaContent {

    void writeTo(OutputStream destination) throws IOException;
}
