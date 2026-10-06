package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

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
}
