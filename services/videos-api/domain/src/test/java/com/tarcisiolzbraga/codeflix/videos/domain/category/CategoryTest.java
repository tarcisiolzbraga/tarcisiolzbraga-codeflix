package com.tarcisiolzbraga.codeflix.videos.domain.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.handler.ThrowsValidationHandler;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CategoryTest {

    private static final CategoryID EXPECTED_ID = CategoryID.from("3f2b1a9c-5d6e-4f70-8a91-b2c3d4e5f607");
    private static final String EXPECTED_NAME = "Filmes";
    private static final String EXPECTED_DESCRIPTION = "A categoria mais assistida";
    private static final Instant EXPECTED_CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant EXPECTED_UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");
    private static final String BLANK_NAME_MESSAGE = "'name' should not be empty";
    private static final String NAME_LENGTH_MESSAGE = "'name' must be between 3 and 255 characters";

    @Test
    void givenValidParams_whenCallWith_thenInstantiateTheReplicaAsReceived() {
        final var actualCategory = aCategory(EXPECTED_NAME);

        assertEquals(EXPECTED_ID, actualCategory.getId());
        assertEquals(EXPECTED_NAME, actualCategory.getName());
        assertEquals(EXPECTED_DESCRIPTION, actualCategory.getDescription());
        assertTrue(actualCategory.isActive());
        assertEquals(EXPECTED_CREATED_AT, actualCategory.getCreatedAt());
        assertEquals(EXPECTED_UPDATED_AT, actualCategory.getUpdatedAt());
    }

    @Test
    void givenAnInactiveCategory_whenCallWith_thenKeepItInactive() {
        final var actualCategory = Category.with(
                EXPECTED_ID, EXPECTED_NAME, EXPECTED_DESCRIPTION, false, EXPECTED_CREATED_AT, EXPECTED_UPDATED_AT);

        assertFalse(actualCategory.isActive());
    }

    @Test
    void givenACategory_whenCallWithCopy_thenReturnAnotherInstanceHoldingTheSameData() {
        final var category = aCategory(EXPECTED_NAME);

        final var actualCopy = Category.with(category);

        assertNotSame(category, actualCopy);
        assertEquals(category.getId(), actualCopy.getId());
        assertEquals(category.getName(), actualCopy.getName());
        assertEquals(category.getDescription(), actualCopy.getDescription());
        assertEquals(category.isActive(), actualCopy.isActive());
        assertEquals(category.getCreatedAt(), actualCopy.getCreatedAt());
        assertEquals(category.getUpdatedAt(), actualCopy.getUpdatedAt());
    }

    @Test
    void givenValidParams_whenCallValidate_thenAccumulateNoError() {
        final var category = aCategory(EXPECTED_NAME);
        final var notification = Notification.create();

        category.validate(notification);

        assertFalse(notification.hasError());
    }

    @Test
    void givenNullDescription_whenCallValidate_thenAccumulateNoErrorBecauseItIsOptional() {
        final var category = Category.with(
                EXPECTED_ID, EXPECTED_NAME, null, true, EXPECTED_CREATED_AT, EXPECTED_UPDATED_AT);
        final var notification = Notification.create();

        category.validate(notification);

        assertFalse(notification.hasError());
        assertNull(category.getDescription());
    }

    @Test
    void givenNullName_whenCallValidate_thenThrowDomainException() {
        final var category = aCategory(null);

        final var actualException =
                assertThrows(DomainException.class, () -> category.validate(new ThrowsValidationHandler()));

        assertEquals("'name' should not be null", actualException.getMessage());
    }

    @Test
    void givenBlankName_whenCallValidate_thenThrowDomainException() {
        final var category = aCategory("   ");

        final var actualException =
                assertThrows(DomainException.class, () -> category.validate(new ThrowsValidationHandler()));

        assertEquals(BLANK_NAME_MESSAGE, actualException.getMessage());
    }

    @Test
    void givenNameShorterThanTheMinimum_whenCallValidate_thenThrowDomainException() {
        final var category = aCategory("Fi ");

        final var actualException =
                assertThrows(DomainException.class, () -> category.validate(new ThrowsValidationHandler()));

        assertEquals(NAME_LENGTH_MESSAGE, actualException.getMessage());
    }

    @Test
    void givenNameLongerThanTheMaximum_whenCallValidate_thenThrowDomainException() {
        final var category = aCategory("a".repeat(256));

        final var actualException =
                assertThrows(DomainException.class, () -> category.validate(new ThrowsValidationHandler()));

        assertEquals(NAME_LENGTH_MESSAGE, actualException.getMessage());
    }



    private static Category aCategory(final String name) {
        return Category.with(
                EXPECTED_ID, name, EXPECTED_DESCRIPTION, true, EXPECTED_CREATED_AT, EXPECTED_UPDATED_AT);
    }
}
