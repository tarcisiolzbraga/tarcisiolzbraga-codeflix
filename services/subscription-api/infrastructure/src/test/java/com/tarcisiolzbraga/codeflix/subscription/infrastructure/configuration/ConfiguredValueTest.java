package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConfiguredValueTest {

    private static final String NAME = "keycloak.issuer-uri";

    @Test
    void givenFilledText_whenChecksIt_thenReturnsTheValueItself() {
        final var value = "codeflix";

        final var actual = ConfiguredValue.text(NAME, value);

        assertSame(value, actual);
    }

    @Test
    void givenNull_whenChecksText_thenSaysItIsRequired() {
        final String value = null;

        final var actual = assertThrows(IllegalStateException.class, () -> ConfiguredValue.text(NAME, value));

        assertEquals("'keycloak.issuer-uri' is required and was not set", actual.getMessage());
    }

    @Test
    void givenBlank_whenChecksText_thenSaysItIsRequired() {
        final var value = "   ";

        final var actual = assertThrows(IllegalStateException.class, () -> ConfiguredValue.text(NAME, value));

        assertEquals("'keycloak.issuer-uri' is required and was not set", actual.getMessage());
    }

    @Test
    void givenUnresolvedPlaceholder_whenChecksText_thenSaysTheVariableWasNotSet() {
        final var value = "${KEYCLOAK_ISSUER_URI}";

        final var actual = assertThrows(IllegalStateException.class, () -> ConfiguredValue.text(NAME, value));

        assertEquals(
                "'keycloak.issuer-uri' was not resolved; got '${KEYCLOAK_ISSUER_URI}'. "
                        + "Set the environment variable.",
                actual.getMessage());
    }

    @Test
    void givenAbsoluteUrl_whenChecksUrl_thenReturnsTheValueItself() {
        final var value = "http://localhost:8081/realms/codeflix";

        final var actual = ConfiguredValue.url(NAME, value);

        assertSame(value, actual);
    }

    @Test
    void givenTextWithoutSchemeOrHost_whenChecksUrl_thenSaysItMustBeAbsolute() {
        final var value = "realms/codeflix";

        final var actual = assertThrows(IllegalStateException.class, () -> ConfiguredValue.url(NAME, value));

        assertEquals("'keycloak.issuer-uri' must be an absolute URL; got 'realms/codeflix'", actual.getMessage());
    }

    @Test
    void givenMissingValue_whenChecksUrl_thenSaysItIsRequiredBeforeLookingAtTheFormat() {
        final String value = null;

        final var actual = assertThrows(IllegalStateException.class, () -> ConfiguredValue.url(NAME, value));

        assertEquals("'keycloak.issuer-uri' is required and was not set", actual.getMessage());
    }
}
