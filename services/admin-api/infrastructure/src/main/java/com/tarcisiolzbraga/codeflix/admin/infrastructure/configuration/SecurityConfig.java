package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.admin.infrastructure.security.KeycloakJwtConverter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
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

    // O actuator mora numa porta própria, e essa porta **não** escapa da segurança: o filtro desta
    // aplicação atravessa o contexto filho do servidor de gerenciamento, e sem esta cadeia a regra
    // geral lá embaixo pedia role de administrador às sondas. O kubelet não carrega token, então o
    // pod nascia eternamente "não pronto" com a aplicação perfeitamente de pé — conferido: as
    // sondas respondiam 401 antes desta cadeia existir, e há teste fixando isso.
    //
    // Liberar aqui não abre nada para fora porque quem decide é a topologia: a porta de
    // gerenciamento não é publicada pelo compose nem roteada pelo gateway, e o que se expõe nela
    // está limitado a health, info e prometheus pelo application.yml.
    @Bean
    @Order(1)
    SecurityFilterChain actuatorFilterChain(final HttpSecurity http) throws Exception {
        return http.securityMatcher(EndpointRequest.toAnyEndpoint())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rules -> rules.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiFilterChain(final HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                // Usa o CorsConfigurationSource que o CorsConfig publica. A verificação prévia do
                // browser é respondida pelo filtro de CORS, antes da autorização — por isso ela não
                // precisa de regra liberando OPTIONS, e há teste provando que não precisa.
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(SecurityConfig::routeRules)
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakJwtConverter())))
                .build();
    }

    private static void routeRules(
            final AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        // O despacho de erro é liberado, e isto não é folga: o Spring Security filtra todos os tipos
        // de despacho, então com `anyRequest()` protegido o encaminhamento para /error entra sem
        // autenticação e é recusado — e o cliente recebe 401 no lugar do 404 ou do 422 de verdade.
        // Liberar o despacho, e não o caminho /error, mantém um GET direto nele fora do ar. A
        // requisição original já passou pela autorização antes de falhar.
        rules.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC)
                .permitAll()
                .requestMatchers(DOCS_PATHS)
                .permitAll()
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
