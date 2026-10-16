package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.IdentityProviderGateway;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.keycloak.ClientCredentials;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.keycloak.KeycloakIdentityProviderClient;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.keycloak.KeycloakTokenSource;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// A conversa com a API administrativa do Keycloak. Os tempos são curtos de propósito: o cadastro
// espera por esta chamada, e é melhor falhar rápido do que prender a requisição de quem se cadastra.
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({KeycloakProperties.class, KeycloakAdminProperties.class})
public class KeycloakAdminConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    // Renova o token quando falta menos que isto para vencer, para ele não morrer no caminho.
    private static final Duration TOKEN_MARGIN = Duration.ofSeconds(30);

    @Bean
    RestClient keycloakRestClient(final RestClient.Builder builder) {
        final var settings = HttpClientSettings.defaults().withTimeouts(CONNECT_TIMEOUT, READ_TIMEOUT);
        return builder.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }

    @Bean
    ClientCredentials keycloakClientCredentials(
            final RestClient keycloakRestClient,
            final KeycloakProperties keycloak,
            final KeycloakAdminProperties admin,
            final Clock clock) {
        return new ClientCredentials(
                new KeycloakTokenSource(keycloakRestClient, keycloak, admin, clock), clock, TOKEN_MARGIN);
    }

    @Bean
    IdentityProviderGateway identityProviderGateway(
            final RestClient keycloakRestClient,
            final KeycloakProperties keycloak,
            final KeycloakAdminProperties admin,
            final ClientCredentials keycloakClientCredentials) {
        return new KeycloakIdentityProviderClient(keycloakRestClient, keycloak, admin, keycloakClientCredentials);
    }
}
