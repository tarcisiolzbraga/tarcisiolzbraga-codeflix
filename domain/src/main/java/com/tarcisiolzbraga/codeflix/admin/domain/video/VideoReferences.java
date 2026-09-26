package com.tarcisiolzbraga.codeflix.admin.domain.video;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import java.util.Objects;
import java.util.Set;

// Os três agregados que o vídeo referencia, sempre só pelo id. Vêm juntos num value object porque
// passá-los soltos estouraria o limite de parâmetros das fábricas do Video.
public record VideoReferences(
        Set<CategoryID> categories, Set<GenreID> genres, Set<CastMemberID> castMembers) {

    public VideoReferences {
        categories = Set.copyOf(Objects.requireNonNull(categories, "'categories' should not be null"));
        genres = Set.copyOf(Objects.requireNonNull(genres, "'genres' should not be null"));
        castMembers = Set.copyOf(Objects.requireNonNull(castMembers, "'castMembers' should not be null"));
    }

    public static VideoReferences with(
            final Set<CategoryID> categories,
            final Set<GenreID> genres,
            final Set<CastMemberID> castMembers) {
        return new VideoReferences(categories, genres, castMembers);
    }

    public static VideoReferences none() {
        return new VideoReferences(Set.of(), Set.of(), Set.of());
    }
}
