package com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models;

import com.tarcisiolzbraga.codeflix.videos.domain.pagination.SearchQuery;

// Os cinco argumentos da query num record só, agrupados pelo @Arguments do Spring GraphQL: o schema
// segue expondo argumentos nomeados com valor padrão, e o método do controller fica com um parâmetro.
public record GqlCastMemberQuery(String search, int page, int perPage, String sort, String direction) {

    public SearchQuery toSearchQuery() {
        return new SearchQuery(this.page, this.perPage, this.search, this.sort, this.direction);
    }
}
