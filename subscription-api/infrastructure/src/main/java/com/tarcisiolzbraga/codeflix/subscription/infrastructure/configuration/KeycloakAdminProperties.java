package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// A credencial com que o serviço administra os usuários do realm, e o grupo em que o assinante
// entra. Separado do KeycloakProperties de propósito: aquele diz onde validar token que chega, este
// diz com que identidade sair chamando a API administrativa — são dois assuntos, e juntá-los faria o
// SecurityConfig depender de credencial que ele não usa.
//
// O grupo é configurado pelo **nome**, não pelo id como na referência: o id é gerado na importação
// do realm, e este realm é descartável — recriar o container trocaria o id e o valor configurado
// apontaria para nada.
@ConfigurationProperties("keycloak.admin")
public record KeycloakAdminProperties(String clientId, String clientSecret, String subscribersGroup) {

    public KeycloakAdminProperties {
        ConfiguredValue.text("keycloak.admin.client-id", clientId);
        ConfiguredValue.text("keycloak.admin.client-secret", clientSecret);
        ConfiguredValue.text("keycloak.admin.subscribers-group", subscribersGroup);
    }
}
