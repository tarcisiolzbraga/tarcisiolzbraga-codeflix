package com.tarcisiolzbraga.codeflix.videos.infrastructure;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

// Pede token ao Keycloak do container pelo fluxo de credenciais de cliente, e guarda por client: o
// token vale minutos, bem mais que a suíte, então uma chamada por client basta.
//
// Cada client do realm de teste carrega um papel diferente, e é assim que o teste escolhe com quem
// está batendo na API: assinante, administrador, ou alguém sem papel algum.
public class KeycloakTestToken {

    public static final String SUBSCRIBER = "catalog-subscriber-test";
    public static final String ADMIN = "catalog-admin-test";
    public static final String STRANGER = "catalog-stranger-test";

    private static final String SECRET = "segredo-de-teste";

    private final String issuerUri;
    private final Map<String, String> cached = new ConcurrentHashMap<>();

    public KeycloakTestToken(final String issuerUri) {
        this.issuerUri = Objects.requireNonNull(issuerUri, "'issuerUri' should not be null");
    }

    public String subscriber() {
        return bearerOf(SUBSCRIBER);
    }

    public String admin() {
        return bearerOf(ADMIN);
    }

    public String bearerOf(final String clientId) {
        return this.cached.computeIfAbsent(clientId, id -> "Bearer " + request(id));
    }

    private String request(final String clientId) {
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", SECRET);
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
