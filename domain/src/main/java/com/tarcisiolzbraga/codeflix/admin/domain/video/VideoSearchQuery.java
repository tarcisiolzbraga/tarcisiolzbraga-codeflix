package com.tarcisiolzbraga.codeflix.admin.domain.video;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import java.util.Objects;
import java.util.Set;

// A paginação comum mais os filtros por agregado referenciado.
public record VideoSearchQuery(
        SearchQuery page,
        Set<CategoryID> categories,
        Set<GenreID> genres,
        Set<CastMemberID> castMembers) {

    public VideoSearchQuery {
        Objects.requireNonNull(page, "'page' should not be null");
        categories = Set.copyOf(Objects.requireNonNull(categories, "'categories' should not be null"));
        genres = Set.copyOf(Objects.requireNonNull(genres, "'genres' should not be null"));
        castMembers = Set.copyOf(Objects.requireNonNull(castMembers, "'castMembers' should not be null"));
    }

    public static VideoSearchQuery with(final SearchQuery page) {
        return new VideoSearchQuery(page, Set.of(), Set.of(), Set.of());
    }
}
