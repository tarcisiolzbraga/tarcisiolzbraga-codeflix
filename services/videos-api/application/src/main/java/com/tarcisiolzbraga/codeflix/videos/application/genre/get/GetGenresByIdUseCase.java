package com.tarcisiolzbraga.codeflix.videos.application.genre.get;

import com.tarcisiolzbraga.codeflix.videos.application.UseCase;
import com.tarcisiolzbraga.codeflix.videos.application.genre.GenreOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import java.util.List;
import java.util.Set;

public abstract class GetGenresByIdUseCase extends UseCase<Set<GenreID>, List<GenreOutput>> {
}
