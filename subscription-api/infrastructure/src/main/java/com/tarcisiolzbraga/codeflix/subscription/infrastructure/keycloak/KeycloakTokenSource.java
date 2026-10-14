package com.tarcisiolzbraga.codeflix.subscription.infrastructure.keycloak;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.InternalErrorException;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration.KeycloakAdminProperties;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration.KeycloakProperties;
import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Pede o token pelo fluxo de credenciais de cliente. É a única coisa aqui que sabe o formato da
// resposta do Keycloak; quem decide quando pedir é o ClientCredentials.
public class KeycloakTokenSource implements ClientCredentials.TokenSource {

    private static final String ACCESS_TOKEN = "access_token";
    private static final String EXPIRES_IN = "expires_in";

    private final RestClient restClient;
    private final KeycloakProperties keycloak;
    private final KeycloakAdminProperties admin;
    private final Clock clock;

    public KeycloakTokenSource(
            final RestClient restClient,
            final KeycloakProperties keycloak,
            final KeycloakAdminProperties admin,
            final Clock clock) {
        this.restClient = Objects.requireNonNull(restClient, "'restClient' should not be null");
        this.keycloak = Objects.requireNonNull(keycloak, "'keycloak' should not be null");
        this.admin = Objects.requireNonNull(admin, "'admin' should not be null");
        this.clock = Objects.requireNonNull(clock, "'clock' should not be null");
    }

    @Override
    public ClientCredentials.Token fetch() {
        final var response = request();
        final var value = asText(response.get(ACCESS_TOKEN), ACCESS_TOKEN);
        final var lifetime = asSeconds(response.get(EXPIRES_IN));
        return new ClientCredentials.Token(value, this.clock.instant().plusSeconds(lifetime));
    }

    private Map<?, ?> request() {
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", this.admin.clientId());
        form.add("client_secret", this.admin.clientSecret());
        try {
            final var body = this.restClient
                    .post()
                    .uri(this.keycloak.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            return Objects.requireNonNull(body, "the token endpoint answered with no body");
        } catch (final RestClientException exception) {
            throw InternalErrorException.with("could not get a token from the identity provider", exception);
        }
    }

    private static String asText(final Object value, final String field) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw InternalErrorException.with("the token response had no '%s'".formatted(field));
        }
        return text;
    }

    // O Keycloak manda expires_in em segundos, e como número. Faltando, assume um minuto: é curto o
    // bastante para renovar logo e evita tratar ausência como token eterno.
    private static long asSeconds(final Object value) {
        return value instanceof Number number ? number.longValue() : 60L;
    }
}
