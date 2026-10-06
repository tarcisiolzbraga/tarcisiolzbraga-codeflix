package com.tarcisiolzbraga.codeflix.videos.infrastructure.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Só o endereço do realm, como no admin-codeflix: o resto se deriva dele, para não haver dois
// lugares dizendo onde o emissor está.
//
// É o MESMO emissor que o admin valida. O `iss` do token é a URL por onde ele foi pedido, então os
// dois lados têm de nomear o Keycloak igual — por isso este endereço e o token-uri do cliente saem
// das mesmas variáveis.
@ConfigurationProperties("keycloak")
public record KeycloakProperties(String issuerUri) {

    public String jwkSetUri() {
        return "%s/protocol/openid-connect/certs".formatted(this.issuerUri);
    }
}
