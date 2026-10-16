package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models;

import com.tarcisiolzbraga.codeflix.videos.application.video.VideoOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import java.util.Set;

// O vídeo como o cliente do catálogo o vê.
//
// Os três campos de ids NÃO estão no schema, de propósito: existem só para os resolvedores em lote
// saberem o que pedir. Como o schema não os declara, nenhum cliente consegue pedi-los, e os ids crus
// — inclusive de relações inativas — nunca saem daqui.
public record GqlVideo(
        String id,
        String title,
        String description,
        Integer yearLaunched,
        Double duration,
        String rating,
        boolean opened,
        String video,
        String trailer,
        String banner,
        String thumbnail,
        String thumbnailHalf,
        Set<CategoryID> categoryIds,
        Set<GenreID> genreIds,
        Set<CastMemberID> castMemberIds) {

    public static GqlVideo from(final VideoOutput output) {
        final var details = output.details();
        final var medias = output.medias();
        final var references = output.references();
        return new GqlVideo(
                output.id(),
                details.title(),
                details.description(),
                details.launchedAt() == null ? null : details.launchedAt().getValue(),
                details.duration(),
                details.rating() == null ? null : details.rating().getLabel(),
                output.flags().opened(),
                medias.video(),
                medias.trailer(),
                medias.banner(),
                medias.thumbnail(),
                medias.thumbnailHalf(),
                references.categories(),
                references.genres(),
                references.castMembers());
    }
}
