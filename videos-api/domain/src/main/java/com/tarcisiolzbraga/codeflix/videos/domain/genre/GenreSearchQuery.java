package com.tarcisiolzbraga.codeflix.videos.domain.genre;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.PageSize;
import java.util.Set;

// A listagem de gêneros filtra também por categoria, então ela tem query própria em vez da
// SearchQuery compartilhada — mesmo critério do admin-codeflix, que reserva uma query por agregado
// só quando há filtro a mais.
public record GenreSearchQuery(
        int page, int perPage, String terms, String sort, String direction, Set<CategoryID> categories) {

    public GenreSearchQuery {
        perPage = PageSize.checked(perPage);
        categories = categories == null ? Set.of() : Set.copyOf(categories);
    }
}
