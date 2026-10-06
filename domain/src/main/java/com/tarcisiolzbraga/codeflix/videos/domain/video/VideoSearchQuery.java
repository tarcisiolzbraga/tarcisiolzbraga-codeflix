package com.tarcisiolzbraga.codeflix.videos.domain.video;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import java.util.Set;

// A listagem de vídeos filtra por classificação, ano e pelas três relações, então tem query própria,
// como a do gênero. Os conjuntos nulos entram vazios: filtro ausente não é erro.
public record VideoSearchQuery(
        int page,
        int perPage,
        String terms,
        String sort,
        String direction,
        Rating rating,
        Integer launchedAt,
        Set<CategoryID> categories,
        Set<GenreID> genres,
        Set<CastMemberID> castMembers) {

    public VideoSearchQuery {
        categories = categories == null ? Set.of() : Set.copyOf(categories);
        genres = genres == null ? Set.of() : Set.copyOf(genres);
        castMembers = castMembers == null ? Set.of() : Set.copyOf(castMembers);
    }
}
