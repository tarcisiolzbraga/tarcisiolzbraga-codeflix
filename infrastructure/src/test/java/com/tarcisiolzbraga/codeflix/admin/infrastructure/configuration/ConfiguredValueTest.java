package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConfiguredValueTest {

    private static final String NAME = "storage.bucket";

    @Test
    void givenAValue_whenCallText_thenReturnIt() {
        final var actualValue = ConfiguredValue.text(NAME, "codeflix-medias");

        assertEquals("codeflix-medias", actualValue);
    }

    @Test
    void givenAnUnresolvedPlaceholder_whenCallText_thenReceiveAnError() {
        final var actualException =
                assertThrows(IllegalStateException.class, () -> ConfiguredValue.text(NAME, "${STORAGE_BUCKET}"));

        assertEquals(
                "'storage.bucket' was not resolved; got '${STORAGE_BUCKET}'. Set the environment variable.",
                actualException.getMessage());
    }

    @Test
    void givenNull_whenCallText_thenReceiveAnError() {
        final var actualException =
                assertThrows(IllegalStateException.class, () -> ConfiguredValue.text(NAME, null));

        assertEquals("'storage.bucket' is required and was not set", actualException.getMessage());
    }

    @Test
    void givenBlank_whenCallText_thenReceiveAnError() {
        assertThrows(IllegalStateException.class, () -> ConfiguredValue.text(NAME, "   "));
    }

    @Test
    void givenAnAbsoluteUrl_whenCallUrl_thenReturnIt() {
        final var actualValue = ConfiguredValue.url(NAME, "http://localhost:3900");

        assertEquals("http://localhost:3900", actualValue);
    }

    @Test
    void givenAPathWithoutHost_whenCallUrl_thenReceiveAnError() {
        final var actualException =
                assertThrows(IllegalStateException.class, () -> ConfiguredValue.url(NAME, "/realms/codeflix"));

        assertEquals("'storage.bucket' must be an absolute URL; got '/realms/codeflix'", actualException.getMessage());
    }

    @Test
    void givenAnUnresolvedPlaceholder_whenCallUrl_thenSayItWasNotResolved() {
        final var actualException = assertThrows(
                IllegalStateException.class, () -> ConfiguredValue.url(NAME, "${KEYCLOAK_ISSUER_URI}"));

        assertEquals(
                "'storage.bucket' was not resolved; got '${KEYCLOAK_ISSUER_URI}'. Set the environment variable.",
                actualException.getMessage());
    }
}
