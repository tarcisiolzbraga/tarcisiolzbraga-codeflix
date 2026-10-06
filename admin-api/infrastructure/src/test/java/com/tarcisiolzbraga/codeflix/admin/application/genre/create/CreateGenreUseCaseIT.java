package com.tarcisiolzbraga.codeflix.admin.application.genre.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.persistence.GenreRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class CreateGenreUseCaseIT {

    private static final String EXPECTED_NAME = "Ação";

    @Autowired
    private CreateGenreUseCase useCase;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private GenreRepository genreRepository;

    @MockitoSpyBean
    private GenreGateway genreGateway;

    @Test
    void givenValidCommandWithCategories_whenCallExecute_thenPersistTheGenreWithThem() {
        final var movies = givenPersistedCategory();
        final var command = CreateGenreCommand.with(EXPECTED_NAME, true, Set.of(movies.getValue()));

        final var actualResult = this.useCase.execute(command);

        assertTrue(actualResult.isRight());
        final var persisted = reload(actualResult.get().id());
        assertEquals(EXPECTED_NAME, persisted.getName());
        assertEquals(Set.of(movies), persisted.getCategories());
        assertTrue(persisted.isActive());
    }

    @Test
    void givenInactiveCommandWithoutCategories_whenCallExecute_thenPersistItInactiveAndEmpty() {
        final var command = CreateGenreCommand.with(EXPECTED_NAME, false, null);

        final var actualResult = this.useCase.execute(command);

        final var persisted = reload(actualResult.get().id());
        assertFalse(persisted.isActive());
        assertTrue(persisted.getCategories().isEmpty());
    }

    @Test
    void givenInvalidNameAndUnknownCategory_whenCallExecute_thenReturnBothErrorsAndPersistNothing() {
        final var unknown = CategoryID.unique().getValue();
        final var command = CreateGenreCommand.with(null, true, Set.of(unknown));

        final var actualResult = this.useCase.execute(command);

        assertTrue(actualResult.isLeft());
        assertEquals(
                List.of("Some categories could not be found: " + unknown, "'name' should not be null"),
                actualResult.getLeft().getErrors().stream().map(ValidationError::message).toList());
        assertEquals(0, this.genreRepository.count());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheExceptionInsteadOfValidationError() {
        final var command = CreateGenreCommand.with(EXPECTED_NAME, true, null);
        final var expectedException = new IllegalStateException("banco indisponível");
        doThrow(expectedException).when(this.genreGateway).create(any());

        final var actualException = assertThrows(IllegalStateException.class, () -> this.useCase.execute(command));

        assertSame(expectedException, actualException);
        assertEquals(0, this.genreRepository.count());
    }

    private CategoryID givenPersistedCategory() {
        return this.categoryGateway.create(Category.newCategory("Filmes", null, true)).getId();
    }

    private Genre reload(final String id) {
        return this.genreGateway.findById(GenreID.from(id)).orElseThrow();
    }
}
