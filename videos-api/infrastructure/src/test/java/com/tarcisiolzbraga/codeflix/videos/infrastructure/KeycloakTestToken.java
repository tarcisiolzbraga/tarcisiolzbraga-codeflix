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
// Os clients são os do realm de verdade, o da raiz do monorepo, e é assim que o teste escolhe com
// quem está batendo na API. O categories-codeflix faz o papel do desconhecido: ele existe no realm
// para provar o 403 nas rotas da admin-api, e para o catálogo ele é justamente alguém identificado
// que não tem papel algum aqui — sem precisar de um client inventado só para isso.
public class KeycloakTestToken {

    public static final String SUBSCRIBER = "subscriber-codeflix";
    public static final String ADMIN = "admin-codeflix";
    public static final String STRANGER = "categories-codeflix";

    // O mesmo valor que o KeycloakContainerConfiguration põe no lugar do marcador do realm.
    public static final String SECRET = "segredo-de-teste";

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
