package com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models;

import com.tarcisiolzbraga.codeflix.videos.domain.pagination.SearchQuery;

// Os cinco argumentos da query num record só. O schema segue expondo argumentos nomeados com valor
// padrão, que é o que serve ao cliente; quem os agrupa é o @Arguments do Spring GraphQL, e assim o
// método do controller fica com um parâmetro em vez de cinco.
public record GqlCategoryQuery(String search, int page, int perPage, String sort, String direction) {

    public SearchQuery toSearchQuery() {
        return new SearchQuery(this.page, this.perPage, this.search, this.sort, this.direction);
    }
}
