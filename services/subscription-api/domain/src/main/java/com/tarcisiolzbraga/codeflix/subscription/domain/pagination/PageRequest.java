package com.tarcisiolzbraga.codeflix.subscription.domain.pagination;

import com.tarcisiolzbraga.codeflix.subscription.domain.AssertionConcern;

// Qual página, e de que tamanho.
//
// Sem `terms`, `sort` e `direction`, que o SearchQuery da admin-api tem: são poucos planos, a ordem
// é sempre pelo nome, e campo que ninguém usa é o erro do `deletedAt` da referência. Entram quando
// houver quem os peça.
//
// O teto do tamanho existe para um cliente não pedir a base inteira numa página e desfazer o
// propósito da paginação.
public record PageRequest(int page, int perPage) implements AssertionConcern {

    public static final int MAX_PER_PAGE = 100;
    private static final int DEFAULT_PER_PAGE = 20;

    public PageRequest {
        if (page < 0) {
            throw com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException.with(
                    "'page' should not be negative");
        }
        if (perPage <= 0) {
            perPage = DEFAULT_PER_PAGE;
        }
        if (perPage > MAX_PER_PAGE) {
            perPage = MAX_PER_PAGE;
        }
    }

    public int offset() {
        return this.page * this.perPage;
    }
}
