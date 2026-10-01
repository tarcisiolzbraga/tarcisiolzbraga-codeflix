package com.tarcisiolzbraga.codeflix.admin.infrastructure;

import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

// Pede um token ao Keycloak do container pelo fluxo de credenciais de cliente, e guarda o valor: o
// token vale minutos, bem mais que a suíte, então uma chamada basta para todos os testes.
public class KeycloakTestToken {

    private final String issuerUri;
    private final String clientId;
    private final String clientSecret;

    private String cached;

    public KeycloakTestToken(final String issuerUri, final String clientId, final String clientSecret) {
        this.issuerUri = Objects.requireNonNull(issuerUri, "'issuerUri' should not be null");
        this.clientId = Objects.requireNonNull(clientId, "'clientId' should not be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "'clientSecret' should not be null");
    }

    public synchronized String bearer() {
        if (this.cached == null) {
            this.cached = "Bearer " + request();
        }
        return this.cached;
    }

    private String request() {
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", this.clientId);
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
