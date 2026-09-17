package com.tarcisiolzbraga.codeflix.admin.domain.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.ThrowsValidationHandler;
import org.junit.jupiter.api.Test;

class CategoryTest {

    private static final String EXPECTED_NAME = "Filmes";
    private static final String EXPECTED_DESCRIPTION = "A categoria mais assistida";

    @Test
    void givenValidParams_whenCallNewCategory_thenInstantiateActiveCategory() {
        final var expectedIsActive = true;

        final var actualCategory = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, expectedIsActive);

        assertNotNull(actualCategory);
        assertNotNull(actualCategory.getId());
        assertEquals(EXPECTED_NAME, actualCategory.getName());
        assertEquals(EXPECTED_DESCRIPTION, actualCategory.getDescription());
        assertEquals(expectedIsActive, actualCategory.isActive());
        assertNotNull(actualCategory.getCreatedAt());
        assertEquals(actualCategory.getCreatedAt(), actualCategory.getUpdatedAt());
        assertNull(actualCategory.getDeletedAt());
    }

    @Test
    void givenInactiveFlag_whenCallNewCategory_thenInstantiateDeletedCategory() {
        final var expectedIsActive = false;

        final var actualCategory = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, expectedIsActive);

        assertFalse(actualCategory.isActive());
        assertNotNull(actualCategory.getDeletedAt());
    }

    @Test
    void givenNullName_whenCallValidate_thenThrowDomainException() {
        final var actualCategory = Category.newCategory(null, EXPECTED_DESCRIPTION, true);

        final var actualException = assertThrows(
                DomainException.class, () -> actualCategory.validate(new ThrowsValidationHandler()));

        assertEquals("'name' should not be null", actualException.getMessage());
    }

    @Test
    void givenBlankName_whenCallValidate_thenThrowDomainException() {
        final var actualCategory = Category.newCategory("   ", EXPECTED_DESCRIPTION, true);

        final var actualException = assertThrows(
                DomainException.class, () -> actualCategory.validate(new ThrowsValidationHandler()));

        assertEquals("'name' should not be empty", actualException.getMessage());
    }

    @Test
    void givenNameShorterThanThreeChars_whenCallValidate_thenThrowDomainException() {
        final var actualCategory = Category.newCategory("Fi ", EXPECTED_DESCRIPTION, true);

        final var actualException = assertThrows(
                DomainException.class, () -> actualCategory.validate(new ThrowsValidationHandler()));

        assertEquals("'name' must be between 3 and 255 characters", actualException.getMessage());
    }

    @Test
    void givenNameLongerThan255Chars_whenCallValidate_thenThrowDomainException() {
        final var actualCategory = Category.newCategory("a".repeat(256), EXPECTED_DESCRIPTION, true);

        final var actualException = assertThrows(
                DomainException.class, () -> actualCategory.validate(new ThrowsValidationHandler()));

        assertEquals("'name' must be between 3 and 255 characters", actualException.getMessage());
    }

    @Test
    void givenActiveCategory_whenCallDeactivate_thenReturnInactiveCategory() {
        final var category = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, true);
        final var createdAt = category.getCreatedAt();

        final var actualCategory = category.deactivate();

        assertFalse(actualCategory.isActive());
        assertNotNull(actualCategory.getDeletedAt());
        assertEquals(createdAt, actualCategory.getCreatedAt());
        assertTrue(actualCategory.getUpdatedAt().isAfter(createdAt));
    }

    @Test
    void givenInactiveCategory_whenCallActivate_thenReturnActiveCategory() {
        final var category = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, false);

        final var actualCategory = category.activate();

        assertTrue(actualCategory.isActive());
        assertNull(actualCategory.getDeletedAt());
    }

    @Test
    void givenValidCategory_whenCallUpdate_thenReturnCategoryUpdated() {
        final var category = Category.newCategory("Serie", "A descrição antiga", true);
        final var updatedAt = category.getUpdatedAt();

        final var actualCategory = category.update(EXPECTED_NAME, EXPECTED_DESCRIPTION, true);

        assertEquals(EXPECTED_NAME, actualCategory.getName());
        assertEquals(EXPECTED_DESCRIPTION, actualCategory.getDescription());
        assertTrue(actualCategory.isActive());
        assertTrue(actualCategory.getUpdatedAt().isAfter(updatedAt));
    }

    @Test
    void givenValidCategory_whenCallUpdateToInactive_thenReturnCategoryDeactivated() {
        final var category = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, true);

        final var actualCategory = category.update(EXPECTED_NAME, EXPECTED_DESCRIPTION, false);

        assertFalse(actualCategory.isActive());
        assertNotNull(actualCategory.getDeletedAt());
    }

    @Test
    void givenTwoCategoriesWithSameId_whenCompareThem_thenBeEqual() {
        final var category = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, true);
        final var sameId = Category.with(
                category.getId(), "Outro nome", "Outra descrição", true,
                category.getCreatedAt(), category.getUpdatedAt(), null);

        final var actualEquals = category.equals(sameId);

        assertTrue(actualEquals);
        assertEquals(category.hashCode(), sameId.hashCode());
    }
}
