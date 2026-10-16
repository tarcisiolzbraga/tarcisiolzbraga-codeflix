package com.tarcisiolzbraga.codeflix.admin.domain.genre;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import java.util.Optional;
import java.util.Set;

public interface GenreGateway {

    Genre create(Genre genre);

    Genre update(Genre genre);

    void deleteById(GenreID id);

    Optional<Genre> findById(GenreID id);

    Pagination<Genre> findAll(SearchQuery query);

    // Resultado limitado pelos IDs recebidos, então não é listagem.
    Set<GenreID> findExistingIds(Set<GenreID> ids);

    boolean existsByCategory(CategoryID categoryId);
}
