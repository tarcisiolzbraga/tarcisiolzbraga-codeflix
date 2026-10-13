package com.tarcisiolzbraga.codeflix.subscription.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntityTest {

    @Test
    void givenTwoEntitiesWithTheSameId_whenComparesThem_thenTheyAreEqual() {
        final var id = new StubId(UUID.randomUUID());

        final var actual = new StubEntity(id).equals(new StubEntity(id));

        assertEquals(true, actual);
    }

    @Test
    void givenTwoEntitiesWithTheSameId_whenComparesTheirHashes_thenTheyMatch() {
        final var id = new StubId(UUID.randomUUID());

        final var actual = new StubEntity(id).hashCode();

        assertEquals(new StubEntity(id).hashCode(), actual);
    }

    @Test
    void givenTwoEntitiesWithDifferentIds_whenComparesThem_thenTheyAreNotEqual() {
        final var one = new StubEntity(new StubId(UUID.randomUUID()));

        final var actual = one.equals(new StubEntity(new StubId(UUID.randomUUID())));

        assertEquals(false, actual);
    }

    @Test
    void givenAnEntity_whenComparesToSomethingElse_thenTheyAreNotEqual() {
        final var one = new StubEntity(new StubId(UUID.randomUUID()));

        final var actual = one.equals("codeflix");

        assertNotEquals(true, actual);
    }

    @Test
    void givenNullId_whenCreatesTheEntity_thenThrowsDomainException() {
        final StubId id = null;

        final var actual = assertThrows(DomainException.class, () -> new StubEntity(id));

        assertEquals("'id' should not be null", actual.getMessage());
    }

    private record StubId(UUID value) implements Identifier {}

    private static final class StubEntity extends Entity<StubId> {
        private StubEntity(final StubId id) {
            super(id);
        }
    }
}
