package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class KeycloakPropertiesTest {

    private static final String ISSUER_URI = "http://localhost:8081/realms/codeflix";

    @Test
    void givenTheIssuerUri_whenAsksForTheJwkSetUri_thenDerivesItFromTheIssuer() {
        final var properties = new KeycloakProperties(ISSUER_URI);

        final var actual = properties.jwkSetUri();

        assertEquals(ISSUER_URI + "/protocol/openid-connect/certs", actual);
    }

    @Test
    void givenNoIssuerUri_whenBindsTheProperties_thenRefusesToStart() {
        final String issuerUri = null;

        final var actual = assertThrows(IllegalStateException.class, () -> new KeycloakProperties(issuerUri));

        assertEquals("'keycloak.issuer-uri' is required and was not set", actual.getMessage());
    }
}
