package com.tarcisiolzbraga.codeflix.admin.application.genre.list;

import com.tarcisiolzbraga.codeflix.admin.application.UseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;

public abstract class ListGenresUseCase extends UseCase<SearchQuery, Pagination<GenreListOutput>> {
}
