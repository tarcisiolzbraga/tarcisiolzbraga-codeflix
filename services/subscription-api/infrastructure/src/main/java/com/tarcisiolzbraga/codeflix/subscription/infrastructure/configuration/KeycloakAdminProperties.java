package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// A credencial com que o serviço administra os usuários do realm, e os dois grupos em que uma pessoa
// pode entrar. Separado do KeycloakProperties de propósito: aquele diz onde validar token que chega,
// este diz com que identidade sair chamando a API administrativa — são dois assuntos, e juntá-los
// faria o SecurityConfig depender de credencial que ele não usa.
//
// Os grupos são configurados pelo **nome**, não pelo id como na referência: o id é gerado na
// importação do realm, e este realm é descartável — recriar o container trocaria o id e o valor
// configurado apontaria para nada.
//
// Dois grupos porque são dois acessos: `accounts` é ter conta aqui, `subscribers` é poder assistir.
// O porquê está no IdentityProviderGateway.
@ConfigurationProperties("keycloak.admin")
public record KeycloakAdminProperties(
        String clientId, String clientSecret, String accountsGroup, String subscribersGroup) {

    public KeycloakAdminProperties {
        ConfiguredValue.text("keycloak.admin.client-id", clientId);
        ConfiguredValue.text("keycloak.admin.client-secret", clientSecret);
        ConfiguredValue.text("keycloak.admin.accounts-group", accountsGroup);
        ConfiguredValue.text("keycloak.admin.subscribers-group", subscribersGroup);
    }
}
