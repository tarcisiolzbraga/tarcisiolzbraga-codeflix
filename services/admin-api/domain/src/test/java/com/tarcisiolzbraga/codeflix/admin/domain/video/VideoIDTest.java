package com.tarcisiolzbraga.codeflix.admin.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VideoIDTest {

    private static final String FIRST = "11111111-1111-1111-1111-111111111111";
    private static final String SECOND = "22222222-2222-2222-2222-222222222222";

    @Test
    void givenSameValue_whenCompareIDs_thenAreEqual() {
        final var first = VideoID.from(FIRST);
        final var second = VideoID.from(FIRST);

        final var actualEquals = first.equals(second);

        assertTrue(actualEquals);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void givenDifferentValues_whenCompareIDs_thenAreNotEqual() {
        final var first = VideoID.from(FIRST);
        final var second = VideoID.from(SECOND);

        final var actualEquals = first.equals(second);

        assertFalse(actualEquals);
    }

    @Test
    void givenUppercaseUUID_whenCallFrom_thenStoreLowercaseValue() {
        final var uuid = UUID.fromString("A1B2C3D4-0000-0000-0000-00000000000F");

        final var actualID = VideoID.from(uuid);

        assertEquals("a1b2c3d4-0000-0000-0000-00000000000f", actualID.getValue());
    }

    // O motivo de o id ser UUID e não String. Antes, from(String) guardava o texto como veio e
    // from(UUID) normalizava, então as duas grafias do mesmo id eram ids diferentes — e o MySQL,
    // que compara ignorando a caixa, escondia isso no banco enquanto a memória discordava.
    @Test
    void givenTheSameUUIDInDifferentCases_whenCallFrom_thenAreTheSameID() {
        final var upper = VideoID.from("A1B2C3D4-0000-0000-0000-00000000000F");
        final var lower = VideoID.from("a1b2c3d4-0000-0000-0000-00000000000f");

        assertEquals(lower, upper);
        assertEquals(lower.getValue(), upper.getValue());
    }

    @Test
    void givenAValueThatIsNotAUUID_whenCallFrom_thenThrowDomainException() {
        final var actualException = assertThrows(DomainException.class, () -> VideoID.from("nao-e-um-uuid"));

        assertEquals("'nao-e-um-uuid' is not a valid VideoID", actualException.getMessage());
    }

    @Test
    void givenNullValue_whenCallFrom_thenThrowNullPointerException() {
        final String value = null;

        final var actualException = assertThrows(NullPointerException.class, () -> VideoID.from(value));

        assertEquals("'value' should not be null", actualException.getMessage());
    }

    @Test
    void givenNoValue_whenCallUnique_thenGenerateLowercaseUUID() {
        final var actualID = VideoID.unique();

        assertEquals(actualID.getValue().toLowerCase(), actualID.getValue());
        assertEquals(36, actualID.getValue().length());
    }
}
