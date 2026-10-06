package com.tarcisiolzbraga.codeflix.videos.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.configuration.properties.KeycloakProperties;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.security.KeycloakJwtConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.SecurityDataFetcherExceptionResolver;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

// O Spring Security está aqui por dois motivos agora: o oauth2-client, que pede token para falar com
// a API do admin, e o resource server, que valida o token de quem fala com esta API.
//
// A autorização é por consulta, e não por rota, porque o GraphQL é um endereço só: no nível HTTP não
// há como distinguir ler o acervo de gravar pela mutation de exemplo. Então /graphql fica liberado no
// filtro e quem decide é o @Secured de cada método do controller — o token, quando vem, é validado
// de todo modo, e token inválido ou expirado é recusado ali no filtro.
//
// Diferente da referência do curso, não há @Profile("!development"): aqui o development é o perfil
// padrão, inclusive dos testes, e desligar a segurança nele deixaria a autorização sem um único
// teste. O preço é que o bootRun local pede token — a página do GraphiQL continua aberta, e o
// cabeçalho Authorization se cola nela.
//
// Sem csrf e sem sessão porque a API é sem estado: quem se identifica é o token de cada requisição.
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@EnableConfigurationProperties(KeycloakProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiFilterChain(final HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rules -> rules.anyRequest().permitAll())
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakJwtConverter())))
                .build();
    }

    // Construído do jwk-set-uri, e não do issuer-uri: o segundo faria o Spring buscar os metadados
    // do emissor na subida, e a aplicação deixaria de subir sem o Keycloak no ar — inclusive nos
    // testes. O validador de emissor entra à mão, que é o que o issuer-uri traria de graça.
    @Bean
    JwtDecoder jwtDecoder(final KeycloakProperties properties) {
        final var decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuerUri()));
        return decoder;
    }

    // Sem isto, a recusa do @Secured chega ao cliente como INTERNAL_ERROR: o Spring GraphQL esconde
    // detalhe de exceção não resolvida. Este resolver do próprio spring-graphql a traduz em
    // UNAUTHORIZED quando ninguém se identificou e FORBIDDEN quando o papel não bastou.
    @Bean
    SecurityDataFetcherExceptionResolver securityExceptionResolver() {
        return new SecurityDataFetcherExceptionResolver();
    }
}
