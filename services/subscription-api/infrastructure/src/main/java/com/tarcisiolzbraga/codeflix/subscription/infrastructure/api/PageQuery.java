package com.tarcisiolzbraga.codeflix.subscription.infrastructure.api;

import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.PageRequest;

// Os parâmetros de paginação como vêm na query string, com padrão para a rota funcionar sem nenhum.
//
// Sem `search`, `sort` e `direction`, que a admin-api tem: são poucos planos, a ordem é sempre pelo
// nome, e parâmetro que ninguém usa é o erro do `deletedAt`. Entram quando houver quem os peça.
public record PageQuery(Integer page, Integer perPage) {

    public PageRequest toPageRequest() {
        // O PageRequest trata tamanho inválido e aplica o teto; aqui só o nulo vira o padrão dele.
        return new PageRequest(this.page == null ? 0 : this.page, this.perPage == null ? 0 : this.perPage);
    }
}
