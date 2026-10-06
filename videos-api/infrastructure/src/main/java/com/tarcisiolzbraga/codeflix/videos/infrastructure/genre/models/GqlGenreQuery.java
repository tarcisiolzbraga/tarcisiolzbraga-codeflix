package com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreSearchQuery;
import java.util.Set;
import java.util.stream.Collectors;

// Os seis argumentos da query num record só, agrupados pelo @Arguments.
public record GqlGenreQuery(
        String search, int page, int perPage, String sort, String direction, Set<String> categories) {

    public GenreSearchQuery toSearchQuery() {
        return new GenreSearchQuery(
                this.page, this.perPage, this.search, this.sort, this.direction, categoryIds());
    }

    private Set<CategoryID> categoryIds() {
        if (this.categories == null) {
            return Set.of();
        }
        return this.categories.stream().map(CategoryID::from).collect(Collectors.toUnmodifiableSet());
    }
}
