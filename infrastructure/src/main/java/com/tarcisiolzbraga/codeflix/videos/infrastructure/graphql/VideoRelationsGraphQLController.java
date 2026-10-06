package com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql;

import com.tarcisiolzbraga.codeflix.videos.application.castmember.CastMemberOutput;
import com.tarcisiolzbraga.codeflix.videos.application.castmember.get.GetCastMembersByIdUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.category.CategoryOutput;
import com.tarcisiolzbraga.codeflix.videos.application.category.get.GetCategoriesByIdUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.genre.GenreOutput;
import com.tarcisiolzbraga.codeflix.videos.application.genre.get.GetGenresByIdUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models.GqlCastMember;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.GqlCategory;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models.GqlGenre;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.GqlVideo;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.stereotype.Controller;

// As três relações do vídeo, resolvidas em lote: as categorias, os gêneros e os membros de elenco de
// toda a página saem em três consultas, não em três por vídeo.
//
// É aqui que a regra do catálogo se cumpre nas três de uma vez, sem código próprio: cada caso de uso
// passa pelo findAllById do seu gateway, que deixa os inativos de fora. O que não volta não entra no
// mapa, e o vídeo sai sem aquela relação.
@Controller
public class VideoRelationsGraphQLController {

    private final GetCategoriesByIdUseCase getCategoriesByIdUseCase;
    private final GetGenresByIdUseCase getGenresByIdUseCase;
    private final GetCastMembersByIdUseCase getCastMembersByIdUseCase;

    public VideoRelationsGraphQLController(
            final GetCategoriesByIdUseCase getCategoriesByIdUseCase,
            final GetGenresByIdUseCase getGenresByIdUseCase,
            final GetCastMembersByIdUseCase getCastMembersByIdUseCase) {
        this.getCategoriesByIdUseCase =
                Objects.requireNonNull(getCategoriesByIdUseCase, "'getCategoriesByIdUseCase' should not be null");
        this.getGenresByIdUseCase =
                Objects.requireNonNull(getGenresByIdUseCase, "'getGenresByIdUseCase' should not be null");
        this.getCastMembersByIdUseCase =
                Objects.requireNonNull(getCastMembersByIdUseCase, "'getCastMembersByIdUseCase' should not be null");
    }

    @BatchMapping(typeName = "Video", field = "categories")
    public Map<GqlVideo, List<GqlCategory>> categories(final List<GqlVideo> videos) {
        final var found = this.getCategoriesByIdUseCase.execute(idsOf(videos, GqlVideo::categoryIds)).stream()
                .collect(Collectors.toMap(CategoryOutput::id, GqlCategory::from));
        return resolve(videos, GqlVideo::categoryIds, CategoryID::getValue, found, GqlCategory::name);
    }

    @BatchMapping(typeName = "Video", field = "genres")
    public Map<GqlVideo, List<GqlGenre>> genres(final List<GqlVideo> videos) {
        final var found = this.getGenresByIdUseCase.execute(idsOf(videos, GqlVideo::genreIds)).stream()
                .collect(Collectors.toMap(GenreOutput::id, GqlGenre::from));
        return resolve(videos, GqlVideo::genreIds, GenreID::getValue, found, GqlGenre::name);
    }

    @BatchMapping(typeName = "Video", field = "castMembers")
    public Map<GqlVideo, List<GqlCastMember>> castMembers(final List<GqlVideo> videos) {
        final var found = this.getCastMembersByIdUseCase.execute(idsOf(videos, GqlVideo::castMemberIds)).stream()
                .collect(Collectors.toMap(CastMemberOutput::id, GqlCastMember::from));
        return resolve(videos, GqlVideo::castMemberIds, CastMemberID::getValue, found, GqlCastMember::name);
    }

    private static <I> Set<I> idsOf(final List<GqlVideo> videos, final Function<GqlVideo, Set<I>> ids) {
        return videos.stream().flatMap(video -> ids.apply(video).stream()).collect(Collectors.toUnmodifiableSet());
    }

    // Ordenado pelo nome, porque os ids vêm num Set e a ordem de iteração não é definida: sem isto a
    // mesma consulta devolveria a relação em ordens diferentes entre chamadas.
    private static <I, T> Map<GqlVideo, List<T>> resolve(
            final List<GqlVideo> videos,
            final Function<GqlVideo, Set<I>> ids,
            final Function<I, String> value,
            final Map<String, T> found,
            final Function<T, String> name) {
        return videos.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        video -> ids.apply(video).stream()
                                .map(value)
                                .map(found::get)
                                .filter(Objects::nonNull)
                                .sorted(Comparator.comparing(name))
                                .toList()));
    }
}
