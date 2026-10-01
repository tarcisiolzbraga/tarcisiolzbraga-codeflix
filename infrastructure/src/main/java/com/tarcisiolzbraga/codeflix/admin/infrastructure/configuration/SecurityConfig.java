package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.admin.infrastructure.security.KeycloakJwtConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

// Cada agregado tem a sua role, e a de administrador abre tudo. A documentação da API fica aberta:
// ela não expõe dado nenhum, e em produção o springdoc já está desligado.
//
// Sem csrf e sem sessão porque a API é sem estado: quem se identifica é o token de cada requisição.
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(KeycloakProperties.class)
public class SecurityConfig {

    private static final String ROLE_ADMIN = "CODEFLIX_ADMIN";
    private static final String ROLE_CATEGORIES = "CODEFLIX_CATEGORIES";
    private static final String ROLE_GENRES = "CODEFLIX_GENRES";
    private static final String ROLE_CAST_MEMBERS = "CODEFLIX_CAST_MEMBERS";
    private static final String ROLE_VIDEOS = "CODEFLIX_VIDEOS";

    private static final String[] DOCS_PATHS = {"/v3/api-docs/**", "/v3/api-docs", "/swagger-ui/**",
            "/swagger-ui.html"};

    @Bean
    SecurityFilterChain apiFilterChain(final HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(SecurityConfig::routeRules)
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakJwtConverter())))
                .build();
    }

    private static void routeRules(
            final AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules.requestMatchers(DOCS_PATHS).permitAll()
                .requestMatchers("/categories/**").hasAnyRole(ROLE_ADMIN, ROLE_CATEGORIES)
                .requestMatchers("/genres/**").hasAnyRole(ROLE_ADMIN, ROLE_GENRES)
                .requestMatchers("/cast-members/**").hasAnyRole(ROLE_ADMIN, ROLE_CAST_MEMBERS)
                .requestMatchers("/videos/**").hasAnyRole(ROLE_ADMIN, ROLE_VIDEOS)
                .anyRequest().hasRole(ROLE_ADMIN);
    }

    // O Boot sabe montar este decoder sozinho: basta configurar spring.security.oauth2.resourceserver
    // .jwt.jwk-set-uri junto de .issuer-uri, e ele constrói a partir das chaves e instala o validador
    // de emissor, sem buscar metadados na subida. Tentamos, e não serve aqui: as condições daquela
    // autoconfiguração são avaliadas antes de os registradores de propriedade dos testes rodarem,
    // então o container do Keycloak teria de deixar de ser bean para virar singleton estático,
    // diferente do MySQL, do Garage e do RabbitMQ.
    //
    // O preço de montar aqui está declarado: o caminho das chaves fica no código, e os
    // OAuth2TokenValidator e JwkSetUriJwtDecoderBuilderCustomizer registrados como bean são
    // ignorados — quem precisar de um validador novo acrescenta nesta linha.
    //
    // Construído do jwk-set-uri, e não do issuer-uri: o segundo faria o Spring buscar os metadados
    // do emissor na subida, e a aplicação deixaria de subir sem o Keycloak no ar — inclusive nos
    // testes.
    @Bean
    JwtDecoder jwtDecoder(final KeycloakProperties properties) {
        final var decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuerUri()));
        return decoder;
    }
}
