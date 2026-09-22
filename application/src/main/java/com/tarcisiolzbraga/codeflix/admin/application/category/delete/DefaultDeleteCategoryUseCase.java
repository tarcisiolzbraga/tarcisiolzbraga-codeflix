package com.tarcisiolzbraga.codeflix.admin.application.category.delete;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import java.util.Objects;

public class DefaultDeleteCategoryUseCase extends DeleteCategoryUseCase {

    private final CategoryGateway categoryGateway;
    private final GenreGateway genreGateway;

    public DefaultDeleteCategoryUseCase(final CategoryGateway categoryGateway, final GenreGateway genreGateway) {
        this.categoryGateway = Objects.requireNonNull(categoryGateway, "'categoryGateway' should not be null");
        this.genreGateway = Objects.requireNonNull(genreGateway, "'genreGateway' should not be null");
    }

    // Categoria em uso não é removida, só desativada; a foreign key sem cascata segura o que escapar daqui.
    @Override
    public void execute(final String input) {
        final var id = CategoryID.from(input);
        if (this.genreGateway.existsByCategory(id)) {
            throw ConflictException.linked(Category.class, id, Genre.class);
        }
        this.categoryGateway.deleteById(id);
    }
}
