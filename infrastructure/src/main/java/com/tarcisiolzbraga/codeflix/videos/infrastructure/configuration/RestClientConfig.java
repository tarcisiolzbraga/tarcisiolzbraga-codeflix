package com.tarcisiolzbraga.codeflix.videos.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.configuration.properties.AdminApiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
// Local, em vez de um scan na Main: só esta configuração usa essas propriedades.
@EnableConfigurationProperties(AdminApiProperties.class)
public class RestClientConfig {

    // AuthorizedClientService, e não a variante ligada à requisição: quem chama a API do admin é o
    // listener do Kafka, que não roda dentro de uma requisição HTTP e não tem sessão nem
    // SecurityContext de onde pendurar o cliente autorizado.
    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            final ClientRegistrationRepository registrations, final OAuth2AuthorizedClientService clients) {
        final var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clients);
        manager.setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return manager;
    }

    // Sem gerenciador de token escrito à mão: o token do client_credentials vive 300 segundos, e é o
    // próprio Spring Security que o pede, guarda e renova quando expira.
    @Bean
    RestClient adminRestClient(
            final RestClient.Builder builder,
            final OAuth2AuthorizedClientManager manager,
            final AdminApiProperties properties) {
        final var interceptor = new OAuth2ClientHttpRequestInterceptor(manager);
        interceptor.setClientRegistrationIdResolver(request -> properties.clientRegistrationId());
        // O tempo de espera vem de spring.http.client no application.yml, que o Boot já aplica ao
        // RestClient.Builder injetado: montar um request factory à mão só para isso seria repetir
        // configuração que o framework já faz.
        return builder.baseUrl(properties.baseUrl()).requestInterceptor(interceptor).build();
    }
}
