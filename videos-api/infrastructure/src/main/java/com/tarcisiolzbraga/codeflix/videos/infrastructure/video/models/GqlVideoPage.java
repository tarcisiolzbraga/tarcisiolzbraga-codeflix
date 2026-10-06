package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models;

import com.tarcisiolzbraga.codeflix.videos.application.video.VideoOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql.models.GqlPageMeta;
import java.util.List;

public record GqlVideoPage(GqlPageMeta meta, List<GqlVideo> items) {

    public GqlVideoPage {
        items = List.copyOf(items);
    }

    public static GqlVideoPage from(final Pagination<VideoOutput> pagination) {
        return new GqlVideoPage(
                GqlPageMeta.from(pagination),
                pagination.items().stream().map(GqlVideo::from).toList());
    }
}
