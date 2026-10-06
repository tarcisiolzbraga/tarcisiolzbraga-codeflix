package com.tarcisiolzbraga.codeflix.videos.domain.genre;

import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// Recebe GenreSearchQuery, e não a SearchQuery compartilhada, porque a listagem de gênero filtra
// também por categoria.
public interface GenreGateway {

    Genre save(Genre genre);

    void deleteById(GenreID id);

    Optional<Genre> findById(GenreID id);

    // Devolve List porque o resultado é limitado pelos ids recebidos: resolve referências que o
    // chamador já tem em mão.
    List<Genre> findAllById(Set<GenreID> ids);

    Pagination<Genre> findAll(GenreSearchQuery query);
}
