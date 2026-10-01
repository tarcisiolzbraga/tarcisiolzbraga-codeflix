package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Só o endereço do realm. O resto se deriva dele, para não haver dois lugares dizendo onde o
// emissor está.
@ConfigurationProperties("keycloak")
public record KeycloakProperties(String issuerUri) {

    private static final String INVALID_ISSUER_MESSAGE =
            "'keycloak.issuer-uri' must be an absolute URL; got '%s'. Set KEYCLOAK_ISSUER_URI.";

    // Diferente do @Value, a ligação de @ConfigurationProperties ignora placeholder que não
    // resolve: sem esta conferência, variável de ambiente faltando viraria o próprio texto
    // "${KEYCLOAK_ISSUER_URI}" e a aplicação subiria para falhar só na primeira validação de token.
    // A regra do projeto é derrubar a subida, como no banco e no armazenamento.
    public KeycloakProperties {
        if (!isAbsoluteUrl(issuerUri)) {
            throw new IllegalStateException(INVALID_ISSUER_MESSAGE.formatted(issuerUri));
        }
    }

    public String jwkSetUri() {
        return "%s/protocol/openid-connect/certs".formatted(this.issuerUri);
    }

    private static boolean isAbsoluteUrl(final String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            final var uri = URI.create(value);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (final IllegalArgumentException ignored) {
            return false;
        }
    }
}
