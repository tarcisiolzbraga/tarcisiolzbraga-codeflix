package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.security.KeycloakJwtConverter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

// O plano é administração: quem o cria e altera é o operador do catálogo. A conta e a assinatura são
// do assinante, e o administrador também alcança as duas. A documentação da API fica aberta: ela não
// expõe dado nenhum, e em produção o springdoc já está desligado.
//
// O cadastro e a confirmação são as rotas sem token, e não poderiam ser outra coisa: quem se cadastra
// ainda não tem usuário no Keycloak para pedir um, e quem clica no link de validação também não.
//
// Ligado em todos os perfis, diferente da referência, que desliga a segurança no development com um
// @Profile("!development"): com ela desligada no perfil em que se desenvolve, o controle de acesso
// nunca é exercitado onde se trabalha, e a primeira vez que roda de verdade é em outro ambiente.
//
// Sem csrf e sem sessão porque a API é sem estado: quem se identifica é o token de cada requisição.
//
// O despacho de erro é liberado, e isto não é folga: o Spring Security filtra todos os tipos de
// despacho, então com `anyRequest()` protegido o encaminhamento para /error entra sem autenticação e
// é recusado — e o cliente recebe 401 no lugar do 404 ou do 422 de verdade. Liberar o despacho, e
// não o caminho /error, mantém um GET direto nele fora do ar. A requisição original já passou pela
// autorização antes de falhar; quem é recusado por falta de token recebe o 401 do entry point, que
// não passa por aqui.
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableConfigurationProperties(KeycloakProperties.class)
public class SecurityConfig {

    private static final String ROLE_ADMIN = "CODEFLIX_ADMIN";

    // A role que estas rotas exigem é a de **conta**, não a de assinante.
    //
    // CODEFLIX_SUBSCRIBER é o que a videos-api exige para servir o catálogo, e exigi-la aqui obrigou,
    // na primeira versão, a concedê-la na confirmação do cadastro — e aí quem só confirmava um
    // e-mail passava a poder assistir sem pagar. São dois acessos diferentes: ter conta aqui, que
    // vem com o cadastro, e poder assistir, que vem e sai com a assinatura.
    private static final String ROLE_ACCOUNT = "CODEFLIX_ACCOUNT";

    // As duas rotas do cadastro, e só elas. Quem se cadastra ainda não tem usuário no Keycloak para
    // pedir token, e quem clica no link de validação também não — é justamente a confirmação que
    // marca o e-mail como conferido. Listadas uma a uma em vez de /accounts/sign-up/**: com o
    // curinga, qualquer rota futura embaixo desse caminho nasceria aberta sem ninguém decidir.
    private static final String[] SIGN_UP_PATHS = {"/accounts/sign-up", "/accounts/sign-up/confirm"};

    private static final String[] DOCS_PATHS = {
        "/v3/api-docs/**", "/v3/api-docs", "/swagger-ui/**", "/swagger-ui.html"
    };

    @Bean
    SecurityFilterChain apiFilterChain(final HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(SecurityConfig::routeRules)
                .oauth2ResourceServer(
                        oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakJwtConverter())))
                .build();
    }

    private static void routeRules(
            final AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC)
                .permitAll()
                .requestMatchers(DOCS_PATHS)
                .permitAll()
                .requestMatchers(HttpMethod.POST, SIGN_UP_PATHS)
                .permitAll()
                .requestMatchers("/plans/**")
                .hasRole(ROLE_ADMIN)
                .requestMatchers("/accounts/**", "/subscriptions/**")
                .hasAnyRole(ROLE_ADMIN, ROLE_ACCOUNT)
                .anyRequest()
                .hasRole(ROLE_ADMIN);
    }

    // O Boot sabe montar este decoder sozinho: basta configurar spring.security.oauth2.resourceserver
    // .jwt.jwk-set-uri junto de .issuer-uri, e ele constrói a partir das chaves e instala o validador
    // de emissor, sem buscar metadados na subida. Não serve aqui, pelo mesmo motivo da admin-api: as
    // condições daquela autoconfiguração são avaliadas antes de os registradores de propriedade dos
    // testes rodarem, então o container do Keycloak teria de deixar de ser bean para virar singleton
    // estático.
    //
    // Construído do jwk-set-uri, e não do issuer-uri: o segundo faria o Spring buscar os metadados do
    // emissor na subida, e a aplicação deixaria de subir sem o Keycloak no ar — inclusive nos testes.
    @Bean
    JwtDecoder jwtDecoder(final KeycloakProperties properties) {
        final var decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuerUri()));
        return decoder;
    }
}
