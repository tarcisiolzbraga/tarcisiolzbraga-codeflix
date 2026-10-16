package com.tarcisiolzbraga.codeflix.videos.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VideoReferencesTest {

    @Test
    void givenTheThreeSets_whenCallWith_thenHoldThem() {
        final var actualReferences = VideoReferences.with(
                Set.of(CategoryID.from("00000009-0000-0000-0000-000000000000")), Set.of(GenreID.from("00000016-0000-0000-0000-000000000000")), Set.of(CastMemberID.from("00000021-0000-0000-0000-000000000000")));

        assertEquals(Set.of(CategoryID.from("00000009-0000-0000-0000-000000000000")), actualReferences.categories());
        assertEquals(Set.of(GenreID.from("00000016-0000-0000-0000-000000000000")), actualReferences.genres());
        assertEquals(Set.of(CastMemberID.from("00000021-0000-0000-0000-000000000000")), actualReferences.castMembers());
    }

    @Test
    void givenNothing_whenCallNone_thenHoldThreeEmptySets() {
        final var actualReferences = VideoReferences.none();

        assertTrue(actualReferences.categories().isEmpty());
        assertTrue(actualReferences.genres().isEmpty());
        assertTrue(actualReferences.castMembers().isEmpty());
    }

    @Test
    void givenNullCategories_whenCallWith_thenThrowNullPointerException() {
        final var actualException = assertThrows(
                NullPointerException.class,
                () -> VideoReferences.with(null, Set.of(), Set.of()));

        assertEquals("'categories' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullGenres_whenCallWith_thenThrowNullPointerException() {
        final var actualException = assertThrows(
                NullPointerException.class, () -> VideoReferences.with(Set.of(), null, Set.of()));

        assertEquals("'genres' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullCastMembers_whenCallWith_thenThrowNullPointerException() {
        final var actualException = assertThrows(
                NullPointerException.class, () -> VideoReferences.with(Set.of(), Set.of(), null));

        assertEquals("'castMembers' should not be null", actualException.getMessage());
    }

    @Test
    void givenMutableSets_whenCallWith_thenCopyThemSoLaterChangesDoNotLeakIn() {
        final var categories = new HashSet<CategoryID>();
        categories.add(CategoryID.from("00000009-0000-0000-0000-000000000000"));
        final var references = VideoReferences.with(categories, Set.of(), Set.of());

        categories.add(CategoryID.from("00000010-0000-0000-0000-000000000000"));

        assertEquals(1, references.categories().size());
    }

    @Test
    void givenReferences_whenChangeTheReturnedSets_thenRefuseTheChange() {
        final var references = VideoReferences.with(Set.of(CategoryID.from("00000009-0000-0000-0000-000000000000")), Set.of(), Set.of());

        final var actualCategories = references.categories();

        assertThrows(UnsupportedOperationException.class, () -> actualCategories.add(CategoryID.from("00000010-0000-0000-0000-000000000000")));
    }
}
