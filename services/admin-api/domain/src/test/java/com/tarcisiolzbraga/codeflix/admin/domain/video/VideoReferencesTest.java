package com.tarcisiolzbraga.codeflix.admin.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VideoReferencesTest {

    @Test
    void givenTheThreeSets_whenCallWith_thenKeepThem() {
        final var category = CategoryID.unique();
        final var genre = GenreID.unique();
        final var member = CastMemberID.unique();

        final var actualReferences = VideoReferences.with(Set.of(category), Set.of(genre), Set.of(member));

        assertEquals(Set.of(category), actualReferences.categories());
        assertEquals(Set.of(genre), actualReferences.genres());
        assertEquals(Set.of(member), actualReferences.castMembers());
    }

    @Test
    void givenNoReference_whenCallNone_thenReturnEmptySets() {
        final var actualReferences = VideoReferences.none();

        assertTrue(actualReferences.categories().isEmpty());
        assertTrue(actualReferences.genres().isEmpty());
        assertTrue(actualReferences.castMembers().isEmpty());
    }

    @Test
    void givenNullSet_whenCallWith_thenThrowNullPointerException() {
        final var actualException = assertThrows(
                NullPointerException.class, () -> VideoReferences.with(null, Set.of(), Set.of()));

        assertEquals("'categories' should not be null", actualException.getMessage());
    }

    @Test
    void givenAMutableSet_whenChangeItAfterwards_thenTheReferencesDoNotChange() {
        final var categories = new HashSet<CategoryID>();
        categories.add(CategoryID.unique());
        final var actualReferences = VideoReferences.with(categories, Set.of(), Set.of());

        categories.add(CategoryID.unique());

        assertEquals(1, actualReferences.categories().size());
    }
}
