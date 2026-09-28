package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoSearchQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record VideoSearchRequest(
        @Schema(description = "Trecho buscado no título") String search,
        @Schema(description = "Página, começando em 0", defaultValue = "0") Integer page,
        @Schema(description = "Itens por página", defaultValue = "10") Integer perPage,
        @Schema(description = "Campo de ordenação", defaultValue = "title") String sort,
        @Schema(description = "Direção da ordenação: asc ou desc", defaultValue = "asc") String dir,
        @Schema(description = "Filtra pelos vídeos ligados a estas categorias") Set<String> categories,
        @Schema(description = "Filtra pelos vídeos ligados a estes gêneros") Set<String> genres,
        @Schema(description = "Filtra pelos vídeos ligados a estes membros de elenco") Set<String> castMembers) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PER_PAGE = 10;
    private static final String DEFAULT_SORT = "title";
    private static final String DEFAULT_DIRECTION = "asc";

    public VideoSearchQuery toSearchQuery() {
        return new VideoSearchQuery(
                toPage(),
                toIds(this.categories, CategoryID::from),
                toIds(this.genres, GenreID::from),
                toIds(this.castMembers, CastMemberID::from));
    }

    private SearchQuery toPage() {
        return new SearchQuery(
                this.page == null ? DEFAULT_PAGE : this.page,
                this.perPage == null ? DEFAULT_PER_PAGE : this.perPage,
                this.search,
                this.sort == null ? DEFAULT_SORT : this.sort,
                this.dir == null ? DEFAULT_DIRECTION : this.dir);
    }

    private <ID> Set<ID> toIds(final Set<String> values, final Function<String, ID> factory) {
        return values == null ? Set.of() : values.stream().map(factory).collect(Collectors.toUnmodifiableSet());
    }
}
