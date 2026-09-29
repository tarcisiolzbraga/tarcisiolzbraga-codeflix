package com.tarcisiolzbraga.codeflix.admin.e2e;

// O arquivo como ele sai de um cliente: nome, tipo e conteúdo juntos, para o DSL não precisar de
// um método de cinco parâmetros.
public record E2EMediaFile(String filename, String contentType, byte[] content) {

    public static E2EMediaFile of(final String filename, final String contentType, final String content) {
        return new E2EMediaFile(filename, contentType, content.getBytes());
    }
}
