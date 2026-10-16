package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoSearchQuery;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Os dez argumentos da query num record só, agrupados pelo @Arguments: o schema segue expondo
// argumentos nomeados com valor padrão, e o método do controller fica com um parâmetro.
public record GqlVideoQuery(
        String search,
        int page,
        int perPage,
        String sort,
        String direction,
        String rating,
        Integer yearLaunched,
        Set<String> categories,
        Set<String> genres,
        Set<String> castMembers) {

    private static final String RATING_MESSAGE = "'rating' should be one of %s"
            .formatted(String.join(", ", Stream.of(Rating.values()).map(Rating::getLabel).toList()));

    public VideoSearchQuery toSearchQuery() {
        return new VideoSearchQuery(
                this.page,
                this.perPage,
                this.search,
                this.sort,
                this.direction,
                ratingFilter(),
                this.yearLaunched,
                idsOf(this.categories, CategoryID::from),
                idsOf(this.genres, GenreID::from),
                idsOf(this.castMembers, CastMemberID::from));
    }

    // Rótulo desconhecido é recusado, não ignorado: tratá-lo como "sem filtro" devolveria o acervo
    // inteiro para quem pediu uma classificação específica, que é o oposto do que o cliente quis.
    private Rating ratingFilter() {
        if (this.rating == null || this.rating.isBlank()) {
            return null;
        }
        return Rating.of(this.rating).orElseThrow(() -> new IllegalArgumentException(RATING_MESSAGE));
    }

    private static <T> Set<T> idsOf(final Set<String> values, final Function<String, T> factory) {
        if (values == null) {
            return Set.of();
        }
        return values.stream().map(factory).collect(Collectors.toUnmodifiableSet());
    }
}
