package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.admin.application.genre.create.CreateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.create.DefaultCreateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.get.DefaultGetGenreByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.get.GetGenreByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.list.DefaultListGenresUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.list.ListGenresUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.update.DefaultUpdateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.genre.update.UpdateGenreUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// O component scan só enxerga a infrastructure, então os casos de uso são registrados aqui.
@Configuration
public class GenreUseCaseConfig {

    private final CategoryGateway categoryGateway;
    private final GenreGateway genreGateway;

    public GenreUseCaseConfig(final CategoryGateway categoryGateway, final GenreGateway genreGateway) {
        this.categoryGateway = categoryGateway;
        this.genreGateway = genreGateway;
    }

    @Bean
    CreateGenreUseCase createGenreUseCase() {
        return new DefaultCreateGenreUseCase(this.categoryGateway, this.genreGateway);
    }

    @Bean
    GetGenreByIdUseCase getGenreByIdUseCase() {
        return new DefaultGetGenreByIdUseCase(this.genreGateway);
    }

    @Bean
    ListGenresUseCase listGenresUseCase() {
        return new DefaultListGenresUseCase(this.genreGateway);
    }

    @Bean
    UpdateGenreUseCase updateGenreUseCase() {
        return new DefaultUpdateGenreUseCase(this.categoryGateway, this.genreGateway);
    }
}
