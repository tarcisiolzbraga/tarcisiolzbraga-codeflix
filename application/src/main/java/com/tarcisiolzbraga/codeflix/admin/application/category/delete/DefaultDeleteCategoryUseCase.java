package com.tarcisiolzbraga.codeflix.admin.application.category.delete;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import java.util.Objects;

public class DefaultDeleteCategoryUseCase extends DeleteCategoryUseCase {

    private final CategoryGateway categoryGateway;
    private final GenreGateway genreGateway;
    private final VideoGateway videoGateway;

    public DefaultDeleteCategoryUseCase(
            final CategoryGateway categoryGateway,
            final GenreGateway genreGateway,
            final VideoGateway videoGateway) {
        this.categoryGateway = Objects.requireNonNull(categoryGateway, "'categoryGateway' should not be null");
        this.genreGateway = Objects.requireNonNull(genreGateway, "'genreGateway' should not be null");
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
    }

    // Categoria em uso não é removida, só desativada; a foreign key sem cascata segura o que escapar daqui.
    @Override
    public void execute(final String input) {
        final var id = CategoryID.from(input);
        if (this.genreGateway.existsByCategory(id)) {
            throw ConflictException.linked(Category.class, id, Genre.class);
        }
        if (this.videoGateway.existsByCategory(id)) {
            throw ConflictException.linked(Category.class, id, Video.class);
        }
        this.categoryGateway.deleteById(id);
    }
}
