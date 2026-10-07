package com.tarcisiolzbraga.codeflix.videos.domain.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CategoryIDTest {

    private static final String EXPECTED_VALUE = "3f2b1a9c-5d6e-4f70-8a91-b2c3d4e5f607";

    @Test
    void givenAValue_whenCallFrom_thenHoldIt() {
        final var actualId = CategoryID.from(EXPECTED_VALUE);

        assertEquals(EXPECTED_VALUE, actualId.getValue());
        assertEquals(UUID.fromString(EXPECTED_VALUE), actualId.value());
    }

    @Test
    void givenNullValue_whenCallFrom_thenThrowNullPointerException() {
        final String value = null;

        final var actualException = assertThrows(NullPointerException.class, () -> CategoryID.from(value));

        assertEquals("'value' should not be null", actualException.getMessage());
    }

    @Test
    void givenTwoIdsWithTheSameValue_whenCompare_thenBeEqual() {
        final var one = CategoryID.from(EXPECTED_VALUE);
        final var other = CategoryID.from(EXPECTED_VALUE);

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }

    // Aqui o id atravessa CDC, API do admin e Elasticsearch. Guardando UUID, a grafia deixa de ser
    // parte da identidade e as três fronteiras não têm como divergir entre si.
    @Test
    void givenTheSameUUIDInDifferentCases_whenCallFrom_thenAreTheSameID() {
        final var upper = CategoryID.from(EXPECTED_VALUE.toUpperCase());
        final var lower = CategoryID.from(EXPECTED_VALUE);

        assertEquals(lower, upper);
        assertEquals(lower.getValue(), upper.getValue());
    }

    @Test
    void givenAValueThatIsNotAUUID_whenCallFrom_thenThrowDomainException() {
        final var actualException = assertThrows(DomainException.class, () -> CategoryID.from("nao-e-um-uuid"));

        assertEquals("'nao-e-um-uuid' is not a valid CategoryID", actualException.getMessage());
    }

    // Antes era o validador do agregado que recusava id em branco, porque o construtor só barrava
    // nulo. Agora não há como construir um: a recusa saiu da validação e virou propriedade do tipo.
    @Test
    void givenABlankValue_whenCallFrom_thenThrowDomainException() {
        final var actualException = assertThrows(DomainException.class, () -> CategoryID.from("  "));

        assertEquals("'  ' is not a valid CategoryID", actualException.getMessage());
    }
}
