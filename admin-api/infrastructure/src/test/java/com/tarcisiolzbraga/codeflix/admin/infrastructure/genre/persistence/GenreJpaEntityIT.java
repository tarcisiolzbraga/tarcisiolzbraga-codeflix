package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryJpaEntity;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@IntegrationTest
class GenreJpaEntityIT {

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void givenGenreWithCategories_whenSaveAndReload_thenKeepAllFields() {
        final var categories = Set.of(existingCategory("Filmes"), existingCategory("Séries"));
        final var genre = Genre.newGenre("Ação", true).addCategories(categories);

        final var actualGenre = saveAndReload(genre);

        assertEquals(genre.getId(), actualGenre.getId());
        assertEquals("Ação", actualGenre.getName());
        assertEquals(categories, actualGenre.getCategories());
        assertEquals(genre.getCreatedAt(), actualGenre.getCreatedAt());
        assertEquals(genre.getUpdatedAt(), actualGenre.getUpdatedAt());
        assertTrue(actualGenre.isActive());
    }

    @Test
    void givenInactiveGenreWithoutCategories_whenSaveAndReload_thenKeepItInactiveAndEmpty() {
        final var genre = Genre.newGenre("Drama", false);

        final var actualGenre = saveAndReload(genre);

        assertFalse(actualGenre.isActive());
        assertTrue(actualGenre.getCategories().isEmpty());
    }

    @Test
    void givenSavedGenre_whenSaveWithoutOneCategory_thenRemoveOnlyThatLink() {
        final var movies = existingCategory("Filmes");
        final var series = existingCategory("Séries");
        final var genre = saveAndReload(Genre.newGenre("Ação", true).addCategories(Set.of(movies, series)));

        final var actualGenre = saveAndReload(genre.removeCategory(series));

        assertEquals(Set.of(movies), actualGenre.getCategories());
    }

    @Test
    void givenCategoryLinkedToGenre_whenDeleteCategory_thenForeignKeyRejectsIt() {
        final var category = existingCategory("Filmes");
        saveAndReload(Genre.newGenre("Ação", true).addCategory(category));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> this.categoryRepository.deleteById(category.getValue()));

        assertTrue(this.categoryRepository.existsById(category.getValue()));
    }

    private CategoryID existingCategory(final String name) {
        final var category = Category.newCategory(name, null, true);
        this.categoryRepository.saveAndFlush(CategoryJpaEntity.from(category));
        return category.getId();
    }

    private Genre saveAndReload(final Genre genre) {
        this.genreRepository.saveAndFlush(GenreJpaEntity.from(genre));
        return this.genreRepository
                .findById(genre.getId().getValue())
                .map(GenreJpaEntity::toAggregate)
                .orElseThrow();
    }
}
