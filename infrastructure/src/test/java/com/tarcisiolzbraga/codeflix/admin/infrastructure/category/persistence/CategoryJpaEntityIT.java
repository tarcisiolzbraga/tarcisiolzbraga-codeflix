package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class CategoryJpaEntityIT {

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanUp() {
        this.categoryRepository.deleteAll();
    }

    @Test
    void givenActiveCategory_whenSaveAndReload_thenKeepAllFields() {
        final var category = Category.newCategory("Filmes", "A mais assistida", true);

        final var actualCategory = saveAndReload(category);

        assertEquals(category.getId(), actualCategory.getId());
        assertEquals("Filmes", actualCategory.getName());
        assertEquals("A mais assistida", actualCategory.getDescription());
        assertEquals(truncate(category.getCreatedAt()), truncate(actualCategory.getCreatedAt()));
        assertNull(actualCategory.getDeletedAt());
    }

    @Test
    void givenInactiveCategory_whenSaveAndReload_thenKeepDeletedAt() {
        final var category = Category.newCategory("Series", null, false);

        final var actualCategory = saveAndReload(category);

        assertFalse(actualCategory.isActive());
        assertNull(actualCategory.getDescription());
        assertNotNull(actualCategory.getDeletedAt());
    }

    private Category saveAndReload(final Category category) {
        this.categoryRepository.saveAndFlush(CategoryJpaEntity.from(category));
        return this.categoryRepository
                .findById(category.getId().getValue())
                .map(CategoryJpaEntity::toAggregate)
                .orElseThrow();
    }

    private Instant truncate(final Instant instant) {
        return instant.truncatedTo(ChronoUnit.MILLIS);
    }
}
