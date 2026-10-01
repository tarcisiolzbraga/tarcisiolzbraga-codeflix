package com.tarcisiolzbraga.codeflix.admin.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

// O emissor para os testes que não precisam de um Keycloak de verdade. Ele é obrigatório desde que
// a aplicação passou a recusar subir sem um emissor válido.
//
// O endereço usa o domínio reservado .invalid, que nunca resolve: se algum teste tentar buscar as
// chaves de assinatura, falha alto em vez de silenciosamente acertar o Keycloak da máquina de quem
// está rodando. Quem precisa de token de verdade sobrescreve isto com o container.
@TestConfiguration(proxyBeanMethods = false)
public class KeycloakIssuerConfiguration {

    static final String ISSUER_URI = "http://keycloak.invalid/realms/codeflix";

    @Bean
    DynamicPropertyRegistrar keycloakIssuerProperty() {
        return registry -> registry.add("keycloak.issuer-uri", () -> ISSUER_URI);
    }
}
