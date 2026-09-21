package com.tarcisiolzbraga.codeflix.admin.domain.genre;

import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import java.util.Optional;

public interface GenreGateway {

    Genre create(Genre genre);

    Genre update(Genre genre);

    void deleteById(GenreID id);

    Optional<Genre> findById(GenreID id);

    Pagination<Genre> findAll(SearchQuery query);
}
