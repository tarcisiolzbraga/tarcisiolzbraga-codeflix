package com.tarcisiolzbraga.codeflix.videos.application.genre.list;

import com.tarcisiolzbraga.codeflix.videos.application.UseCase;
import com.tarcisiolzbraga.codeflix.videos.application.genre.GenreOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreSearchQuery;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;

public abstract class ListGenresUseCase extends UseCase<GenreSearchQuery, Pagination<GenreOutput>> {
}
