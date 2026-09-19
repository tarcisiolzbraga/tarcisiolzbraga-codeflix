package com.tarcisiolzbraga.codeflix.admin.application.category.list;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import java.util.Objects;

public class DefaultListCategoriesUseCase extends ListCategoriesUseCase {

    private final CategoryGateway categoryGateway;

    public DefaultListCategoriesUseCase(final CategoryGateway categoryGateway) {
        this.categoryGateway = Objects.requireNonNull(categoryGateway, "'categoryGateway' should not be null");
    }

    @Override
    public Pagination<CategoryListOutput> execute(final SearchQuery input) {
        return this.categoryGateway.findAll(input).map(CategoryListOutput::from);
    }
}
