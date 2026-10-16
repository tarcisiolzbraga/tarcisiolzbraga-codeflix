package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Deixa o front do assinante, que roda noutra origem, ler a resposta desta API no browser.
//
// **CORS não é segurança desta API: é política do browser.** Quem autoriza aqui é o token de cada
// requisição, e recusar uma origem não impede chamada nenhuma de fora do browser — curl, Postman e
// outro serviço ignoram tudo isto. O que esta configuração faz é parar de o browser **esconder** a
// resposta de um front legítimo.
//
// Por isso a origem vem de configuração, e não é `*`: liberar tudo não deixaria a API menos segura,
// mas esconderia o dia em que a origem de produção não foi configurada. Com lista explícita esse
// erro aparece na primeira tela, que é onde ele sai barato.
//
// Sem credencial: o token vai no cabeçalho Authorization, que o browser não manda sozinho, e cookie
// nenhum participa. É isso que permite liberar qualquer cabeçalho de requisição sem abrir brecha —
// a especificação só proíbe o curinga de cabeçalho quando há credencial.
//
// OPTIONS não entra na lista de métodos de propósito: na requisição de verificação prévia o que é
// conferido é o método que vem no Access-Control-Request-Method, não o OPTIONS em si, e quem
// responde a ela é o filtro de CORS, antes da autorização. Listar OPTIONS sugeriria que a API tem
// uma rota dele.
//
// O Location é exposto porque o browser esconde cabeçalho de resposta que não seja simples, e as
// criações desta API respondem 201 apontando para o recurso criado. Sem isto o front lê `undefined`
// num cabeçalho que chegou — defeito que se procura no lugar errado.
@Configuration(proxyBeanMethods = false)
public class CorsConfig {

    // Todas as rotas: não há caminho que o front alcance e outro que não.
    private static final String EVERY_PATH = "/**";

    // Meia hora de verificação prévia em cache: o browser repete o OPTIONS a cada rota nova, e a
    // política aqui não muda em tempo de execução.
    private static final Duration PREFLIGHT_CACHE = Duration.ofMinutes(30);

    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE");

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${cors.allowed-origins}") final List<String> allowedOrigins) {
        this.allowedOrigins = requireOrigins(allowedOrigins);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        final var policy = new CorsConfiguration();
        policy.setAllowedOrigins(allowedOrigins);
        policy.setAllowedMethods(ALLOWED_METHODS);
        policy.setAllowedHeaders(List.of(CorsConfiguration.ALL));
        policy.setExposedHeaders(List.of(HttpHeaders.LOCATION));
        policy.setAllowCredentials(false);
        policy.setMaxAge(PREFLIGHT_CACHE);
        final var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(EVERY_PATH, policy);
        return source;
    }

    // Derruba a subida, como o resto da configuração deste serviço faz: `CORS_ALLOWED_ORIGINS=` no
    // ambiente chega aqui como uma lista de um texto vazio, e sem esta conferência a aplicação subiria
    // com uma política que recusa toda origem — falhando só no browser de quem for usar.
    private static List<String> requireOrigins(final List<String> origins) {
        if (origins == null || origins.isEmpty() || origins.stream().anyMatch(String::isBlank)) {
            throw new IllegalStateException("'cors.allowed-origins' is required and must not contain blank entries");
        }
        return List.copyOf(origins);
    }
}
