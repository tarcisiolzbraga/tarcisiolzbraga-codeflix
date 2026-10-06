package com.tarcisiolzbraga.codeflix.videos.domain.pagination;

public record SearchQuery(int page, int perPage, String terms, String sort, String direction) {

    public SearchQuery {
        perPage = PageSize.checked(perPage);
    }
}
