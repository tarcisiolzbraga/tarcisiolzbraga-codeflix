package com.tarcisiolzbraga.codeflix.videos.domain.category;

import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.SearchQuery;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// O save está aqui, e não contradiz a Category ser réplica: este lado não cria categoria, ele
// guarda no próprio armazenamento a versão que o admin-codeflix publicou.
public interface CategoryGateway {

    Category save(Category category);

    void deleteById(CategoryID id);

    Optional<Category> findById(CategoryID id);

    // Devolve List, e não Pagination, porque o resultado é limitado pelos ids recebidos: não é
    // listagem, é resolver um punhado de referências que o chamador já tem em mão.
    List<Category> findAllById(Set<CategoryID> ids);

    Pagination<Category> findAll(SearchQuery query);
}
