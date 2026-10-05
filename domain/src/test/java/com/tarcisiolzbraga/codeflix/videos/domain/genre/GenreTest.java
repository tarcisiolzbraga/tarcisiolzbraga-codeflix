package com.tarcisiolzbraga.codeflix.videos.domain.genre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.handler.ThrowsValidationHandler;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GenreTest {

    private static final GenreID EXPECTED_ID = GenreID.from("5b8d2e1f-3a4c-4d60-9e71-f2a3b4c5d6e7");
    private static final String EXPECTED_NAME = "Ação";
    private static final Set<CategoryID> EXPECTED_CATEGORIES =
            Set.of(CategoryID.from("c1"), CategoryID.from("c2"));
    private static final Instant EXPECTED_CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant EXPECTED_UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");
    private static final String NAME_LENGTH_MESSAGE = "'name' must be between 3 and 255 characters";

    @Test
    void givenValidParams_whenCallWith_thenInstantiateTheReplicaAsReceived() {
        final var actualGenre = aGenre(EXPECTED_NAME, EXPECTED_CATEGORIES);

        assertEquals(EXPECTED_ID, actualGenre.getId());
        assertEquals(EXPECTED_NAME, actualGenre.getName());
        assertEquals(EXPECTED_CATEGORIES, actualGenre.getCategories());
        assertTrue(actualGenre.isActive());
        assertEquals(EXPECTED_CREATED_AT, actualGenre.getCreatedAt());
        assertEquals(EXPECTED_UPDATED_AT, actualGenre.getUpdatedAt());
    }

    @Test
    void givenAGenreWithoutCategories_whenCallWith_thenHoldAnEmptySet() {
        final var actualGenre = aGenre(EXPECTED_NAME, Set.of());

        assertTrue(actualGenre.getCategories().isEmpty());
    }

    @Test
    void givenNullCategories_whenCallWith_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> aGenre(EXPECTED_NAME, null));

        assertEquals("'categories' should not be null", actualException.getMessage());
    }

    @Test
    void givenAMutableSetOfCategories_whenCallWith_thenCopyItSoLaterChangesDoNotLeakIn() {
        final var categories = new HashSet<CategoryID>();
        categories.add(CategoryID.from("c1"));
        final var genre = aGenre(EXPECTED_NAME, categories);

        categories.add(CategoryID.from("c2"));

        assertEquals(1, genre.getCategories().size());
    }

    @Test
    void givenAGenre_whenChangeTheReturnedCategories_thenRefuseTheChange() {
        final var genre = aGenre(EXPECTED_NAME, EXPECTED_CATEGORIES);

        final var actualCategories = genre.getCategories();

        assertThrows(UnsupportedOperationException.class, () -> actualCategories.add(CategoryID.from("c3")));
    }

    @Test
    void givenAnInactiveGenre_whenCallWith_thenKeepItInactive() {
        final var actualGenre = Genre.with(
                EXPECTED_ID, EXPECTED_NAME, false, EXPECTED_CATEGORIES, EXPECTED_CREATED_AT, EXPECTED_UPDATED_AT);

        assertFalse(actualGenre.isActive());
    }

    @Test
    void givenAGenre_whenCallWithCopy_thenReturnAnotherInstanceHoldingTheSameData() {
        final var genre = aGenre(EXPECTED_NAME, EXPECTED_CATEGORIES);

        final var actualCopy = Genre.with(genre);

        assertNotSame(genre, actualCopy);
        assertEquals(genre.getId(), actualCopy.getId());
        assertEquals(genre.getName(), actualCopy.getName());
        assertEquals(genre.getCategories(), actualCopy.getCategories());
        assertEquals(genre.isActive(), actualCopy.isActive());
        assertEquals(genre.getCreatedAt(), actualCopy.getCreatedAt());
        assertEquals(genre.getUpdatedAt(), actualCopy.getUpdatedAt());
    }

    @Test
    void givenValidParams_whenCallValidate_thenAccumulateNoError() {
        final var genre = aGenre(EXPECTED_NAME, EXPECTED_CATEGORIES);
        final var notification = Notification.create();

        genre.validate(notification);

        assertFalse(notification.hasError());
    }

    @Test
    void givenNullName_whenCallValidate_thenThrowDomainException() {
        final var genre = aGenre(null, EXPECTED_CATEGORIES);

        final var actualException =
                assertThrows(DomainException.class, () -> genre.validate(new ThrowsValidationHandler()));

        assertEquals("'name' should not be null", actualException.getMessage());
    }

    @Test
    void givenBlankName_whenCallValidate_thenThrowDomainException() {
        final var genre = aGenre("   ", EXPECTED_CATEGORIES);

        final var actualException =
                assertThrows(DomainException.class, () -> genre.validate(new ThrowsValidationHandler()));

        assertEquals("'name' should not be empty", actualException.getMessage());
    }

    @Test
    void givenNameShorterThanTheMinimum_whenCallValidate_thenThrowDomainException() {
        final var genre = aGenre("Aç", EXPECTED_CATEGORIES);

        final var actualException =
                assertThrows(DomainException.class, () -> genre.validate(new ThrowsValidationHandler()));

        assertEquals(NAME_LENGTH_MESSAGE, actualException.getMessage());
    }

    @Test
    void givenNameLongerThanTheMaximum_whenCallValidate_thenThrowDomainException() {
        final var genre = aGenre("a".repeat(256), EXPECTED_CATEGORIES);

        final var actualException =
                assertThrows(DomainException.class, () -> genre.validate(new ThrowsValidationHandler()));

        assertEquals(NAME_LENGTH_MESSAGE, actualException.getMessage());
    }

    @Test
    void givenBlankId_whenCallValidate_thenThrowDomainExceptionBecauseTheMessageCameCorrupted() {
        final var genre = Genre.with(
                GenreID.from("  "),
                EXPECTED_NAME,
                true,
                EXPECTED_CATEGORIES,
                EXPECTED_CREATED_AT,
                EXPECTED_UPDATED_AT);

        final var actualException =
                assertThrows(DomainException.class, () -> genre.validate(new ThrowsValidationHandler()));

        assertEquals("'id' should not be empty", actualException.getMessage());
    }

    @Test
    void givenBlankIdAndBlankName_whenCallValidateWithNotification_thenAccumulateBothErrors() {
        final var genre = Genre.with(
                GenreID.from(""), "  ", true, EXPECTED_CATEGORIES, EXPECTED_CREATED_AT, EXPECTED_UPDATED_AT);
        final var notification = Notification.create();

        genre.validate(notification);

        assertEquals(2, notification.getErrors().size());
        assertEquals("'id' should not be empty", notification.firstError().orElseThrow().message());
        assertEquals("'name' should not be empty", notification.getErrors().get(1).message());
    }

    private static Genre aGenre(final String name, final Set<CategoryID> categories) {
        return Genre.with(EXPECTED_ID, name, true, categories, EXPECTED_CREATED_AT, EXPECTED_UPDATED_AT);
    }
}
