package com.tarcisiolzbraga.codeflix.admin.domain.video;

import java.io.IOException;
import java.io.OutputStream;

// O conteúdo de uma mídia que **ainda não foi lido**: quem o quer diz onde escrevê-lo.
//
// Existe para o arquivo não precisar caber na memória. Um pedido com `Range` de 10 MB chegava a nove
// cópias do mesmo trecho entre os dois serviços — duas no SDK do S3, duas em cada record que o
// carregava (construtor e accessor, por cópia defensiva), e três no catálogo. Trocar o `byte[]` por
// esta função deixa a leitura acontecer **durante** a escrita da resposta, com um buffer de
// transferência em vez do trecho inteiro.
//
// É uma função, e não um `InputStream`, de propósito: `InputStream` no domínio traria um recurso com
// ciclo de vida para dentro de um lugar que não sabe fechá-lo. Aqui quem abre também fecha, dentro do
// `writeTo`, e o domínio só sabe que existe algo a escrever.
//
// A assinatura é a mesma do `StreamingResponseBody` do Spring de propósito: a rota passa o
// `writeTo` adiante sem embrulho nenhum.
@FunctionalInterface
public interface MediaContent {

    void writeTo(OutputStream destination) throws IOException;
}
