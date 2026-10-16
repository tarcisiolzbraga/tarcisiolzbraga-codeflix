package com.tarcisiolzbraga.codeflix.videos.domain.video;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import java.util.Objects;
import java.util.Set;

// As três relações do vídeo, guardadas só pelo id, como no Genre. Quem as resolve é a borda, pelos
// gateways de cada agregado — e é lá que os inativos ficam de fora, nas três de uma vez.
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
