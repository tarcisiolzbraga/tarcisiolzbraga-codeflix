package com.tarcisiolzbraga.codeflix.videos.application.category.get;

import com.tarcisiolzbraga.codeflix.videos.application.category.CategoryOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DefaultGetCategoriesByIdUseCase extends GetCategoriesByIdUseCase {

    private final CategoryGateway categoryGateway;

    public DefaultGetCategoriesByIdUseCase(final CategoryGateway categoryGateway) {
        this.categoryGateway = Objects.requireNonNull(categoryGateway, "'categoryGateway' should not be null");
    }

    @Override
    public List<CategoryOutput> execute(final Set<CategoryID> input) {
        Objects.requireNonNull(input, "'input' should not be null");
        if (input.isEmpty()) {
            return List.of();
        }

        return this.categoryGateway.findAllById(input).stream().map(CategoryOutput::from).toList();
    }
}
