package com.tarcisiolzbraga.codeflix.admin.application.genre.delete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryRepository;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.persistence.GenreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class DeleteGenreUseCaseIT {

    @Autowired
    private DeleteGenreUseCase useCase;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoSpyBean
    private GenreGateway genreGateway;

    @Test
    void givenGenreWithCategories_whenCallExecute_thenRemoveItAndKeepTheCategories() {
        final var genre = givenPersistedGenre();

        this.useCase.execute(genre.getId().getValue());

        assertEquals(0, this.genreRepository.count());
        assertEquals(1, this.categoryRepository.count());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenDoNothing() {
        givenPersistedGenre();

        this.useCase.execute(GenreID.unique().getValue());

        assertEquals(1, this.genreRepository.count());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var genre = givenPersistedGenre();
        final var expectedException = new IllegalStateException("banco indisponível");
        doThrow(expectedException).when(this.genreGateway).deleteById(any());

        final var actualException =
                assertThrows(IllegalStateException.class, () -> this.useCase.execute(genre.getId().getValue()));

        assertSame(expectedException, actualException);
        assertEquals(1, this.genreRepository.count());
    }

    private Genre givenPersistedGenre() {
        final var movies = this.categoryGateway.create(Category.newCategory("Filmes", null, true)).getId();
        return this.genreGateway.create(Genre.newGenre("Ação", true).addCategory(movies));
    }
}
