package com.tarcisiolzbraga.codeflix.admin.infrastructure.storage;

// O cliente pediu um trecho que o arquivo não tem.
//
// Nasce aqui, e não no domínio, porque é falha técnica da conversa com o armazenamento — como um
// tempo esgotado. Quem a traduz em 416 é a rota, que é quem conhece HTTP.
public class RangeNotSatisfiableException extends RuntimeException {

    public RangeNotSatisfiableException(final String range, final Throwable cause) {
        super("o trecho pedido não existe no arquivo: %s".formatted(range), cause);
    }
}
