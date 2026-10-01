package com.tarcisiolzbraga.codeflix.admin.infrastructure;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

// Pede token ao Keycloak do container pelo fluxo de credenciais de cliente, e guarda por client: o
// token vale minutos, bem mais que a suíte, então uma chamada por client basta.
public class KeycloakTestToken {

    private final String issuerUri;
    private final String defaultClientId;
    private final String clientSecret;
    private final Map<String, String> cached = new ConcurrentHashMap<>();

    public KeycloakTestToken(final String issuerUri, final String defaultClientId, final String clientSecret) {
        this.issuerUri = Objects.requireNonNull(issuerUri, "'issuerUri' should not be null");
        this.defaultClientId = Objects.requireNonNull(defaultClientId, "'defaultClientId' should not be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "'clientSecret' should not be null");
    }

    public String bearer() {
        return bearerOf(this.defaultClientId);
    }

    public String bearerOf(final String clientId) {
        return this.cached.computeIfAbsent(clientId, id -> "Bearer " + request(id));
    }

    private String request(final String clientId) {
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", this.clientSecret);
        final var response = RestClient.create()
                .post()
                .uri(this.issuerUri + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        return String.valueOf(Objects.requireNonNull(response, "no token response").get("access_token"));
    }
}
