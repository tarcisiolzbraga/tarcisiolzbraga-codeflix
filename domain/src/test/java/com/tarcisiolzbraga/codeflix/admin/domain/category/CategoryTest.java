package com.tarcisiolzbraga.codeflix.admin.domain.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.ThrowsValidationHandler;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CategoryTest {

    private static final String EXPECTED_NAME = "Filmes";
    private static final String EXPECTED_DESCRIPTION = "A categoria mais assistida";
    private static final int NANOS_PER_MICRO = 1_000;

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
    }

    @Test
    void givenInactiveFlag_whenCallNewCategory_thenInstantiateInactiveCategory() {
        final var expectedIsActive = false;

        final var actualCategory = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, expectedIsActive);

        assertFalse(actualCategory.isActive());
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
    void givenNameWithExactlyThreeChars_whenCallValidate_thenHaveNoErrors() {
        final var category = Category.newCategory("Fil", EXPECTED_DESCRIPTION, true);
        final var notification = Notification.create();

        category.validate(notification);

        assertTrue(notification.getErrors().isEmpty());
    }

    @Test
    void givenNameWithExactly255Chars_whenCallValidate_thenHaveNoErrors() {
        final var category = Category.newCategory("a".repeat(255), EXPECTED_DESCRIPTION, true);
        final var notification = Notification.create();

        category.validate(notification);

        assertTrue(notification.getErrors().isEmpty());
    }

    @Test
    void givenNullDescription_whenCallValidate_thenHaveNoErrors() {
        final var category = Category.newCategory(EXPECTED_NAME, null, true);
        final var notification = Notification.create();

        category.validate(notification);

        assertTrue(notification.getErrors().isEmpty());
    }

    @Test
    void givenBlankDescription_whenCallValidate_thenHaveNoErrors() {
        final var category = Category.newCategory(EXPECTED_NAME, "   ", true);
        final var notification = Notification.create();

        category.validate(notification);

        assertTrue(notification.getErrors().isEmpty());
    }

    @Test
    void givenInactiveFlag_whenCallNewCategory_thenTruncateTimestampsToMicroseconds() {
        final var expectedIsActive = false;

        final var actualCategory = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, expectedIsActive);

        assertTrue(hasMicrosecondPrecision(actualCategory.getCreatedAt()));
        assertTrue(hasMicrosecondPrecision(actualCategory.getUpdatedAt()));
    }

    @Test
    void givenActiveCategory_whenCallDeactivate_thenTruncateUpdatedAtToMicroseconds() {
        final var category = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, true);

        category.deactivate();

        assertTrue(hasMicrosecondPrecision(category.getUpdatedAt()));
    }

    @Test
    void givenInactiveCategory_whenCallUpdate_thenTruncateUpdatedAtToMicroseconds() {
        final var category = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, false);

        final var actualCategory = category.update(EXPECTED_NAME, EXPECTED_DESCRIPTION);

        assertTrue(hasMicrosecondPrecision(actualCategory.getUpdatedAt()));
    }

    @Test
    void givenNullCreatedAt_whenCallWith_thenThrowNullPointerException() {
        final var id = CategoryID.unique();
        final var updatedAt = InstantUtils.now();

        final var actualException = assertThrows(
                NullPointerException.class,
                () -> Category.with(id, EXPECTED_NAME, EXPECTED_DESCRIPTION, true, null, updatedAt));

        assertEquals("'createdAt' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullUpdatedAt_whenCallWith_thenThrowNullPointerException() {
        final var id = CategoryID.unique();
        final var createdAt = InstantUtils.now();

        final var actualException = assertThrows(
                NullPointerException.class,
                () -> Category.with(id, EXPECTED_NAME, EXPECTED_DESCRIPTION, true, createdAt, null));

        assertEquals("'updatedAt' should not be null", actualException.getMessage());
    }

    @Test
    void givenActiveCategory_whenCallDeactivate_thenTurnItInactive() {
        final var category = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, true);
        final var createdAt = category.getCreatedAt();

        category.deactivate();

        assertFalse(category.isActive());
        assertEquals(createdAt, category.getCreatedAt());
        assertTrue(category.getUpdatedAt().isAfter(createdAt));
    }

    @Test
    void givenInactiveCategory_whenCallActivate_thenTurnItActive() {
        final var category = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, false);

        category.activate();

        assertTrue(category.isActive());
        assertTrue(category.getUpdatedAt().isAfter(category.getCreatedAt()));
    }

    @Test
    void givenValidCategory_whenCallUpdate_thenReturnCategoryUpdated() {
        final var category = Category.newCategory("Serie", "A descrição antiga", true);
        final var updatedAt = category.getUpdatedAt();

        final var actualCategory = category.update(EXPECTED_NAME, EXPECTED_DESCRIPTION);

        assertEquals(EXPECTED_NAME, actualCategory.getName());
        assertEquals(EXPECTED_DESCRIPTION, actualCategory.getDescription());
        assertTrue(actualCategory.isActive());
        assertTrue(actualCategory.getUpdatedAt().isAfter(updatedAt));
    }

    @Test
    void givenInactiveCategory_whenCallUpdate_thenKeepItInactive() {
        final var category = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, false);

        final var actualCategory = category.update(EXPECTED_NAME, EXPECTED_DESCRIPTION);

        assertEquals(EXPECTED_NAME, actualCategory.getName());
        assertFalse(actualCategory.isActive());
    }

    @Test
    void givenTwoCategoriesWithSameId_whenCompareThem_thenBeEqual() {
        final var category = Category.newCategory(EXPECTED_NAME, EXPECTED_DESCRIPTION, true);
        final var sameId = Category.with(
                category.getId(), "Outro nome", "Outra descrição", true,
                category.getCreatedAt(), category.getUpdatedAt());

        final var actualEquals = category.equals(sameId);

        assertTrue(actualEquals);
        assertEquals(category.hashCode(), sameId.hashCode());
    }

    private boolean hasMicrosecondPrecision(final Instant instant) {
        return instant.getNano() % NANOS_PER_MICRO == 0;
    }
}
