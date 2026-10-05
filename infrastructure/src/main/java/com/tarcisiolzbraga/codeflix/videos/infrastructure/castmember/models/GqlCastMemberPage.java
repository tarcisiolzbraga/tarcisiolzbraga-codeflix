package com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models;

import com.tarcisiolzbraga.codeflix.videos.application.castmember.CastMemberOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql.models.GqlPageMeta;
import java.util.List;

public record GqlCastMemberPage(GqlPageMeta meta, List<GqlCastMember> items) {

    public GqlCastMemberPage {
        items = List.copyOf(items);
    }

    public static GqlCastMemberPage from(final Pagination<CastMemberOutput> pagination) {
        return new GqlCastMemberPage(
                GqlPageMeta.from(pagination),
                pagination.items().stream().map(GqlCastMember::from).toList());
    }
}
