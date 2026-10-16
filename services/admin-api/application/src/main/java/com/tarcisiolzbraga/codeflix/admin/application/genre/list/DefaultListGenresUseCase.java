package com.tarcisiolzbraga.codeflix.admin.application.genre.list;

import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import java.util.Objects;

public class DefaultListGenresUseCase extends ListGenresUseCase {

    private final GenreGateway genreGateway;

    public DefaultListGenresUseCase(final GenreGateway genreGateway) {
        this.genreGateway = Objects.requireNonNull(genreGateway, "'genreGateway' should not be null");
    }

    @Override
    public Pagination<GenreListOutput> execute(final SearchQuery input) {
        return this.genreGateway.findAll(input).map(GenreListOutput::from);
    }
}
