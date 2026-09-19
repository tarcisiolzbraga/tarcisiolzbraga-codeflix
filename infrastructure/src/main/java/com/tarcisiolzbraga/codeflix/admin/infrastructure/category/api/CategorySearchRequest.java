package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.api;

import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;

public record CategorySearchRequest(String search, Integer page, Integer perPage, String sort, String dir) {

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
