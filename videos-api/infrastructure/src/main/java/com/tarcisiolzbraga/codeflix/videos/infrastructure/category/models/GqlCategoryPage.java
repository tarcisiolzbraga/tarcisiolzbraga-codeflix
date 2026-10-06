package com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models;

import com.tarcisiolzbraga.codeflix.videos.application.category.CategoryOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql.models.GqlPageMeta;
import java.util.List;

// A página sai com os números ao lado dos itens, e não como lista crua: sem o total o cliente
// pagina no escuro, sem saber quando acabou.
public record GqlCategoryPage(GqlPageMeta meta, List<GqlCategory> items) {

    public GqlCategoryPage {
        items = List.copyOf(items);
    }

    public static GqlCategoryPage from(final Pagination<CategoryOutput> pagination) {
        return new GqlCategoryPage(
                GqlPageMeta.from(pagination),
                pagination.items().stream().map(GqlCategory::from).toList());
    }
}
