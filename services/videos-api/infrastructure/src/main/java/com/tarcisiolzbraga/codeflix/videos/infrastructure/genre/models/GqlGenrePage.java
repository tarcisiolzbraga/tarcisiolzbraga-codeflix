package com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models;

import com.tarcisiolzbraga.codeflix.videos.application.genre.GenreOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql.models.GqlPageMeta;
import java.util.List;

public record GqlGenrePage(GqlPageMeta meta, List<GqlGenre> items) {

    public GqlGenrePage {
        items = List.copyOf(items);
    }

    public static GqlGenrePage from(final Pagination<GenreOutput> pagination) {
        return new GqlGenrePage(
                GqlPageMeta.from(pagination),
                pagination.items().stream().map(GqlGenre::from).toList());
    }
}
