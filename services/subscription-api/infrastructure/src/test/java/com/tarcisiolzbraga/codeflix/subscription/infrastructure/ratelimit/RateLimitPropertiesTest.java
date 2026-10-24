package com.tarcisiolzbraga.codeflix.subscription.infrastructure.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.ratelimit.RateLimitProperties.Tier;
import java.time.Duration;
import org.junit.jupiter.api.Test;

// O que acontece quando a configuração não diz nada é a parte que importa aqui: um limitador que
// nasce desligado por omissão é pior que nenhum, porque parece estar lá.
class RateLimitPropertiesTest {

    @Test
    void givenNothingConfigured_whenBuilt_thenIsEnabledWithTheDefaultTiers() {
        final var actual = new RateLimitProperties(null, null, null, null);

        assertTrue(actual.enabled());
        assertEquals(10, actual.signUp().capacity());
        assertEquals(20, actual.confirm().capacity());
        assertEquals(120, actual.general().capacity());
        assertEquals(Duration.ofMinutes(1), actual.general().period());
    }

    // A coleção do Postman faz 4 chamadas a sign-up e 2 a confirm, e precisa rodar repetidas vezes.
    // Este teste é o que impede alguém de apertar o teto e quebrar a regressão manual sem perceber:
    // com teto de 5 ela tomava 429 em si mesma, e foi assim que o número virou 10.
    @Test
    void givenTheDefaultTiers_whenThePostmanCollectionRunsTwice_thenItStillFitsInThem() {
        final var actual = new RateLimitProperties(null, null, null, null);

        assertTrue(actual.signUp().capacity() >= 2 * 4, "sign-up: " + actual.signUp().capacity());
        assertTrue(actual.confirm().capacity() >= 2 * 2, "confirm: " + actual.confirm().capacity());
    }

    @Test
    void givenTiersWithDifferentPeriods_whenAsksForTheLongest_thenAnswerIt() {
        final var actual = new RateLimitProperties(
                null,
                new Tier(1, Duration.ofMinutes(10)),
                new Tier(1, Duration.ofMinutes(30)),
                new Tier(1, Duration.ofMinutes(1)));

        assertEquals(Duration.ofMinutes(30), actual.longestPeriod());
    }

    @Test
    void givenDisabledOnPurpose_whenBuilt_thenStaysDisabled() {
        final var actual = new RateLimitProperties(false, null, null, null);

        assertFalse(actual.enabled());
    }

    @Test
    void givenACapacityThatIsNotPositive_whenBuilt_thenRefuseIt() {
        final var actual = assertThrows(IllegalStateException.class, () -> new Tier(0, Duration.ofMinutes(1)));

        assertTrue(actual.getMessage().contains("capacity"), actual.getMessage());
    }

    @Test
    void givenAPeriodThatIsNotPositive_whenBuilt_thenRefuseIt() {
        final var actual = assertThrows(IllegalStateException.class, () -> new Tier(5, Duration.ZERO));

        assertTrue(actual.getMessage().contains("period"), actual.getMessage());
    }
}
