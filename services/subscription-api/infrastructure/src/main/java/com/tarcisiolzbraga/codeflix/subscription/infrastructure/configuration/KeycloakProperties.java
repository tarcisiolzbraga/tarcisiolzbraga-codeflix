package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Só o endereço do realm. O resto se deriva dele, para não haver dois lugares dizendo onde o
// emissor está.
@ConfigurationProperties("keycloak")
public record KeycloakProperties(String issuerUri) {

    public KeycloakProperties {
        ConfiguredValue.url("keycloak.issuer-uri", issuerUri);
    }

    public String jwkSetUri() {
        return "%s/protocol/openid-connect/certs".formatted(this.issuerUri);
    }

    public String tokenUri() {
        return "%s/protocol/openid-connect/token".formatted(this.issuerUri);
    }

    // A API administrativa fica no mesmo servidor, com /admin antes do realm. Derivada daqui, e não
    // configurada à parte, pelo mesmo motivo do jwk-set-uri: dois lugares dizendo onde o Keycloak
    // está divergem, e foi uma divergência dessas que já fez o admin recusar o token do catálogo.
    public String adminRealmUri() {
        final var marker = "/realms/";
        final var at = this.issuerUri.lastIndexOf(marker);
        if (at < 0) {
            throw new IllegalStateException(
                    "'keycloak.issuer-uri' should point at a realm; got '%s'".formatted(this.issuerUri));
        }
        return this.issuerUri.substring(0, at) + "/admin" + this.issuerUri.substring(at);
    }
}
