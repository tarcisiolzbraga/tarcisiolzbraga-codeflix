package com.tarcisiolzbraga.codeflix.admin.domain.genre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.ThrowsValidationHandler;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GenreTest {

    private static final String EXPECTED_NAME = "Ação";
    private static final int NANOS_PER_MICRO = 1_000;
    private static final String CATEGORY_ID_NOT_NULL_MESSAGE = "'categoryID' should not be null";

    @Test
    void givenValidParams_whenCallNewGenre_thenInstantiateActiveGenre() {
        final var expectedIsActive = true;

        final var actualGenre = Genre.newGenre(EXPECTED_NAME, expectedIsActive);

        assertNotNull(actualGenre);
        assertNotNull(actualGenre.getId());
        assertEquals(EXPECTED_NAME, actualGenre.getName());
        assertEquals(expectedIsActive, actualGenre.isActive());
        assertTrue(actualGenre.getCategories().isEmpty());
        assertNotNull(actualGenre.getCreatedAt());
        assertEquals(actualGenre.getCreatedAt(), actualGenre.getUpdatedAt());
    }

    @Test
    void givenInactiveFlag_whenCallNewGenre_thenInstantiateInactiveGenre() {
        final var expectedIsActive = false;

        final var actualGenre = Genre.newGenre(EXPECTED_NAME, expectedIsActive);

        assertFalse(actualGenre.isActive());
    }

    @Test
    void givenValidParams_whenCallNewGenre_thenTruncateTimestampsToMicroseconds() {
        final var actualGenre = Genre.newGenre(EXPECTED_NAME, true);

        assertTrue(hasMicrosecondPrecision(actualGenre.getCreatedAt()));
        assertTrue(hasMicrosecondPrecision(actualGenre.getUpdatedAt()));
    }

    @Test
    void givenNullName_whenCallValidate_thenThrowDomainException() {
        final var actualGenre = Genre.newGenre(null, true);

        final var actualException = assertThrows(
                DomainException.class, () -> actualGenre.validate(new ThrowsValidationHandler()));

        assertEquals("'name' should not be null", actualException.getMessage());
    }

    @Test
    void givenBlankName_whenCallValidate_thenThrowDomainException() {
        final var actualGenre = Genre.newGenre("   ", true);

        final var actualException = assertThrows(
                DomainException.class, () -> actualGenre.validate(new ThrowsValidationHandler()));

        assertEquals("'name' should not be empty", actualException.getMessage());
    }

    @Test
    void givenNameLongerThan255Chars_whenCallValidate_thenThrowDomainException() {
        final var actualGenre = Genre.newGenre("a".repeat(256), true);

        final var actualException = assertThrows(
                DomainException.class, () -> actualGenre.validate(new ThrowsValidationHandler()));

        assertEquals("'name' must be between 1 and 255 characters", actualException.getMessage());
    }

    @Test
    void givenNameWithExactlyOneChar_whenCallValidate_thenHaveNoErrors() {
        final var genre = Genre.newGenre("a", true);
        final var notification = Notification.create();

        genre.validate(notification);

        assertTrue(notification.getErrors().isEmpty());
    }

    @Test
    void givenNameWithExactly255Chars_whenCallValidate_thenHaveNoErrors() {
        final var genre = Genre.newGenre("a".repeat(255), true);
        final var notification = Notification.create();

        genre.validate(notification);

        assertTrue(notification.getErrors().isEmpty());
    }

    @Test
    void givenNullCreatedAt_whenCallWith_thenThrowNullPointerException() {
        final var id = GenreID.unique();
        final var updatedAt = InstantUtils.now();

        final var actualException = assertThrows(
                NullPointerException.class,
                () -> Genre.with(id, EXPECTED_NAME, true, Set.of(), null, updatedAt));

        assertEquals("'createdAt' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullUpdatedAt_whenCallWith_thenThrowNullPointerException() {
        final var id = GenreID.unique();
        final var createdAt = InstantUtils.now();

        final var actualException = assertThrows(
                NullPointerException.class,
                () -> Genre.with(id, EXPECTED_NAME, true, Set.of(), createdAt, null));

        assertEquals("'updatedAt' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullCategories_whenCallWith_thenThrowNullPointerException() {
        final var id = GenreID.unique();
        final var now = InstantUtils.now();

        final var actualException = assertThrows(
                NullPointerException.class,
                () -> Genre.with(id, EXPECTED_NAME, true, null, now, now));

        assertEquals("'categories' should not be null", actualException.getMessage());
    }

    @Test
    void givenGenreWithCategories_whenCallWith_thenCopyTheReceivedCategories() {
        final var categories = Set.of(CategoryID.unique(), CategoryID.unique());
        final var now = InstantUtils.now();

        final var actualGenre = Genre.with(GenreID.unique(), EXPECTED_NAME, true, categories, now, now);

        assertEquals(categories, actualGenre.getCategories());
    }

    @Test
    void givenGenreWithCategories_whenCallGetCategories_thenReturnImmutableSet() {
        final var genre = Genre.newGenre(EXPECTED_NAME, true);
        final var categories = genre.getCategories();
        final var categoryID = CategoryID.unique();

        final var actualException = assertThrows(
                UnsupportedOperationException.class, () -> categories.add(categoryID));

        assertNotNull(actualException);
    }

    @Test
    void givenActiveGenre_whenCallDeactivate_thenTurnItInactive() {
        final var genre = Genre.newGenre(EXPECTED_NAME, true);
        final var createdAt = genre.getCreatedAt();

        genre.deactivate();

        assertFalse(genre.isActive());
        assertEquals(createdAt, genre.getCreatedAt());
        assertTrue(genre.getUpdatedAt().isAfter(createdAt));
    }

    @Test
    void givenInactiveGenre_whenCallActivate_thenTurnItActive() {
        final var genre = Genre.newGenre(EXPECTED_NAME, false);

        genre.activate();

        assertTrue(genre.isActive());
        assertTrue(genre.getUpdatedAt().isAfter(genre.getCreatedAt()));
    }

    @Test
    void givenValidGenre_whenCallUpdate_thenReturnGenreUpdated() {
        final var genre = Genre.newGenre("Acao", true);
        final var updatedAt = genre.getUpdatedAt();
        final var expectedCategories = Set.of(CategoryID.unique());

        final var actualGenre = genre.update(EXPECTED_NAME, expectedCategories);

        assertEquals(EXPECTED_NAME, actualGenre.getName());
        assertEquals(expectedCategories, actualGenre.getCategories());
        assertTrue(actualGenre.isActive());
        assertTrue(actualGenre.getUpdatedAt().isAfter(updatedAt));
        assertTrue(hasMicrosecondPrecision(actualGenre.getUpdatedAt()));
    }

    @Test
    void givenInactiveGenre_whenCallUpdate_thenKeepItInactive() {
        final var genre = Genre.newGenre(EXPECTED_NAME, false);

        final var actualGenre = genre.update(EXPECTED_NAME, Set.of());

        assertEquals(EXPECTED_NAME, actualGenre.getName());
        assertFalse(actualGenre.isActive());
    }

    @Test
    void givenGenreWithCategories_whenCallUpdateWithoutThem_thenClearTheCategories() {
        final var genre = Genre.newGenre(EXPECTED_NAME, true).update(EXPECTED_NAME, Set.of(CategoryID.unique()));

        final var actualGenre = genre.update(EXPECTED_NAME, Set.of());

        assertTrue(actualGenre.getCategories().isEmpty());
    }

    @Test
    void givenNullCategories_whenCallUpdate_thenThrowNullPointerException() {
        final var genre = Genre.newGenre(EXPECTED_NAME, true);

        final var actualException = assertThrows(
                NullPointerException.class, () -> genre.update(EXPECTED_NAME, null));

        assertEquals("'categories' should not be null", actualException.getMessage());
    }

    @Test
    void givenTwoGenresWithSameId_whenCompareThem_thenBeEqual() {
        final var genre = Genre.newGenre(EXPECTED_NAME, true);
        final var sameId = Genre.with(
                genre.getId(), "Outro nome", false, Set.of(),
                genre.getCreatedAt(), genre.getUpdatedAt());

        final var actualEquals = genre.equals(sameId);

        assertTrue(actualEquals);
        assertEquals(genre.hashCode(), sameId.hashCode());
    }

    @Test
    void givenGenreWithoutCategories_whenCallAddCategory_thenAddIt() {
        final var genre = Genre.newGenre(EXPECTED_NAME, true);
        final var updatedAt = genre.getUpdatedAt();
        final var expectedCategory = CategoryID.unique();

        final var actualGenre = genre.addCategory(expectedCategory);

        assertEquals(Set.of(expectedCategory), actualGenre.getCategories());
        assertTrue(actualGenre.getUpdatedAt().isAfter(updatedAt));
    }

    @Test
    void givenGenreWithCategory_whenCallAddCategoryAgain_thenKeepASingleEntry() {
        final var expectedCategory = CategoryID.unique();
        final var genre = Genre.newGenre(EXPECTED_NAME, true).addCategory(expectedCategory);

        final var actualGenre = genre.addCategory(expectedCategory);

        assertEquals(Set.of(expectedCategory), actualGenre.getCategories());
    }

    @Test
    void givenNullCategoryID_whenCallAddCategory_thenThrowNullPointerException() {
        final var genre = Genre.newGenre(EXPECTED_NAME, true);

        final var actualException = assertThrows(NullPointerException.class, () -> genre.addCategory(null));

        assertEquals(CATEGORY_ID_NOT_NULL_MESSAGE, actualException.getMessage());
    }

    @Test
    void givenGenreWithoutCategories_whenCallAddCategories_thenAddAllOfThem() {
        final var genre = Genre.newGenre(EXPECTED_NAME, true);
        final var updatedAt = genre.getUpdatedAt();
        final var expectedCategories = Set.of(CategoryID.unique(), CategoryID.unique());

        final var actualGenre = genre.addCategories(expectedCategories);

        assertEquals(expectedCategories, actualGenre.getCategories());
        assertTrue(actualGenre.getUpdatedAt().isAfter(updatedAt));
    }

    @Test
    void givenEmptyCategories_whenCallAddCategories_thenKeepTheGenreUntouched() {
        final var genre = Genre.newGenre(EXPECTED_NAME, true);
        final var updatedAt = genre.getUpdatedAt();

        final var actualGenre = genre.addCategories(Set.of());

        assertTrue(actualGenre.getCategories().isEmpty());
        assertEquals(updatedAt, actualGenre.getUpdatedAt());
    }

    @Test
    void givenNullCategories_whenCallAddCategories_thenThrowNullPointerException() {
        final var genre = Genre.newGenre(EXPECTED_NAME, true);

        final var actualException = assertThrows(NullPointerException.class, () -> genre.addCategories(null));

        assertEquals("'categories' should not be null", actualException.getMessage());
    }

    @Test
    void givenGenreWithCategories_whenCallRemoveCategory_thenRemoveOnlyIt() {
        final var expectedCategory = CategoryID.unique();
        final var removedCategory = CategoryID.unique();
        final var genre = Genre.newGenre(EXPECTED_NAME, true)
                .addCategories(Set.of(expectedCategory, removedCategory));

        final var actualGenre = genre.removeCategory(removedCategory);

        assertEquals(Set.of(expectedCategory), actualGenre.getCategories());
    }

    @Test
    void givenCategoryNotInTheGenre_whenCallRemoveCategory_thenKeepTheCategories() {
        final var expectedCategory = CategoryID.unique();
        final var genre = Genre.newGenre(EXPECTED_NAME, true).addCategory(expectedCategory);

        final var actualGenre = genre.removeCategory(CategoryID.unique());

        assertEquals(Set.of(expectedCategory), actualGenre.getCategories());
    }

    @Test
    void givenNullCategoryID_whenCallRemoveCategory_thenThrowNullPointerException() {
        final var genre = Genre.newGenre(EXPECTED_NAME, true);

        final var actualException = assertThrows(NullPointerException.class, () -> genre.removeCategory(null));

        assertEquals(CATEGORY_ID_NOT_NULL_MESSAGE, actualException.getMessage());
    }

    private boolean hasMicrosecondPrecision(final Instant instant) {
        return instant.getNano() % NANOS_PER_MICRO == 0;
    }
}
