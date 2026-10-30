package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

// O que acontece quando a configuração não diz nada é o que importa aqui: até este trabalho não
// havia bloco `signup:` no application.yml, então os dois prazos vinham só do padrão — e um padrão
// ausente de retenção significa dado pessoal guardado para sempre.
class SignUpPropertiesTest {

    @Test
    void givenNothingConfigured_whenBindsTheProperties_thenFallsBackToADayAndAWeek() {
        final var actual = new SignUpProperties(null, null);

        assertEquals(Duration.ofHours(24), actual.validationTtl());
        assertEquals(Duration.ofDays(7), actual.retention());
    }

    @Test
    void givenBothConfigured_whenBindsTheProperties_thenKeepsThem() {
        final var actual = new SignUpProperties(Duration.ofHours(2), Duration.ofDays(30));

        assertEquals(Duration.ofHours(2), actual.validationTtl());
        assertEquals(Duration.ofDays(30), actual.retention());
    }

    // A retenção precisa ser maior que o prazo do link, senão a linha sai antes de a resposta de
    // "o link venceu" poder acontecer. O padrão respeita isso: uma semana contra um dia.
    @Test
    void givenTheDefaults_whenCompared_thenRetentionOutlivesTheLink() {
        final var actual = new SignUpProperties(null, null);

        assertTrue(actual.retention().compareTo(actual.validationTtl()) > 0);
    }

    @Test
    void givenARetentionThatIsNotPositive_whenBindsTheProperties_thenRefusesIt() {
        final var actual =
                assertThrows(IllegalStateException.class, () -> new SignUpProperties(null, Duration.ZERO));

        assertTrue(actual.getMessage().contains("signup.retention"), actual.getMessage());
    }

    @Test
    void givenAValidationTtlThatIsNotPositive_whenBindsTheProperties_thenRefusesIt() {
        final var actual =
                assertThrows(IllegalStateException.class, () -> new SignUpProperties(Duration.ZERO, null));

        assertTrue(actual.getMessage().contains("signup.validation-ttl"), actual.getMessage());
    }
}
