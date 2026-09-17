package com.tarcisiolzbraga.codeflix.admin.domain.category;

import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import java.util.Optional;

public interface CategoryGateway {

    Category create(Category category);

    Category update(Category category);

    void deleteById(CategoryID id);

    Optional<Category> findById(CategoryID id);

    Pagination<Category> findAll(SearchQuery query);
}
