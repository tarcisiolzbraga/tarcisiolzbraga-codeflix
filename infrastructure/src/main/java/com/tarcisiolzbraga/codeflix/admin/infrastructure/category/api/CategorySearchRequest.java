package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.api;

import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import io.swagger.v3.oas.annotations.media.Schema;

public record CategorySearchRequest(
        @Schema(description = "Trecho buscado no nome ou na descrição") String search,
        @Schema(description = "Página, começando em 0", defaultValue = "0") Integer page,
        @Schema(description = "Itens por página", defaultValue = "10") Integer perPage,
        @Schema(description = "Campo de ordenação", defaultValue = "name") String sort,
        @Schema(description = "Direção da ordenação: asc ou desc", defaultValue = "asc") String dir) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PER_PAGE = 10;
    private static final String DEFAULT_SORT = "name";
    private static final String DEFAULT_DIRECTION = "asc";

    public SearchQuery toSearchQuery() {
        return new SearchQuery(
                this.page == null ? DEFAULT_PAGE : this.page,
                this.perPage == null ? DEFAULT_PER_PAGE : this.perPage,
                this.search,
                this.sort == null ? DEFAULT_SORT : this.sort,
                this.dir == null ? DEFAULT_DIRECTION : this.dir);
    }
}
