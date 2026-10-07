package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class CategoryJpaEntityIT {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void givenActiveCategory_whenSaveAndReload_thenKeepAllFields() {
        final var category = Category.newCategory("Filmes", "A mais assistida", true);

        final var actualCategory = saveAndReload(category);

        assertEquals(category.getId(), actualCategory.getId());
        assertEquals("Filmes", actualCategory.getName());
        assertEquals("A mais assistida", actualCategory.getDescription());
        assertEquals(category.getCreatedAt(), actualCategory.getCreatedAt());
        assertEquals(category.getUpdatedAt(), actualCategory.getUpdatedAt());
        assertTrue(actualCategory.isActive());
    }

    @Test
    void givenInactiveCategory_whenSaveAndReload_thenKeepItInactive() {
        final var category = Category.newCategory("Series", null, false);

        final var actualCategory = saveAndReload(category);

        assertFalse(actualCategory.isActive());
        assertNull(actualCategory.getDescription());
    }

    private Category saveAndReload(final Category category) {
        this.categoryRepository.saveAndFlush(CategoryJpaEntity.from(category));
        return this.categoryRepository
                .findById(category.getId().value())
                .map(CategoryJpaEntity::toAggregate)
                .orElseThrow();
    }
}
