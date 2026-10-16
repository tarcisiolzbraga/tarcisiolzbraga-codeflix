package com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql.models;

import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;

// Os números da página, compartilhados por todos os agregados: é o Pagination do domínio sem os
// itens, que cada agregado tipa do seu jeito no schema.
public record GqlPageMeta(int currentPage, int perPage, long total) {

    public static GqlPageMeta from(final Pagination<?> pagination) {
        return new GqlPageMeta(pagination.currentPage(), pagination.perPage(), pagination.total());
    }
}
