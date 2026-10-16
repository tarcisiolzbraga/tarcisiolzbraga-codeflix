package com.tarcisiolzbraga.codeflix.videos.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.videos.application.genre.delete.DefaultDeleteGenreUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.genre.delete.DeleteGenreUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.genre.get.DefaultGetGenresByIdUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.genre.get.GetGenresByIdUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.genre.list.DefaultListGenresUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.genre.list.ListGenresUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.genre.save.DefaultSaveGenreUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.genre.save.SaveGenreUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreGateway;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GenreUseCaseConfig {

    private final GenreGateway genreGateway;

    public GenreUseCaseConfig(final GenreGateway genreGateway) {
        this.genreGateway = Objects.requireNonNull(genreGateway, "'genreGateway' should not be null");
    }

    @Bean
    SaveGenreUseCase saveGenreUseCase() {
        return new DefaultSaveGenreUseCase(this.genreGateway);
    }

    @Bean
    DeleteGenreUseCase deleteGenreUseCase() {
        return new DefaultDeleteGenreUseCase(this.genreGateway);
    }

    @Bean
    ListGenresUseCase listGenresUseCase() {
        return new DefaultListGenresUseCase(this.genreGateway);
    }

    @Bean
    GetGenresByIdUseCase getGenresByIdUseCase() {
        return new DefaultGetGenresByIdUseCase(this.genreGateway);
    }
}
