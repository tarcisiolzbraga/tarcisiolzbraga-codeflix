package com.tarcisiolzbraga.codeflix.subscription.infrastructure;

import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

// Emite token DE UMA PESSOA, com o `sub` dela, que é o que as rotas `/me` exigem: elas identificam
// quem chama pelo token, e token de client não tem pessoa nenhuma por trás.
//
// Para isso é preciso um client que aceite usuário e senha, e nenhum client do realm versionado
// aceita — nem deveria: o fluxo de senha direta foi depreciado no OAuth 2.1, e um front de navegador
// deve usar código de autorização com PKCE. Então este client é criado **aqui**, no container de
// teste, pelo admin do bootstrap, e não existe em provisioning/keycloak/realm.json. O realm de
// produção continua sem fluxo de senha.
public class KeycloakTestUserToken {

    private static final String E2E_CLIENT_ID = "e2e-test-client";
    private static final String MASTER_REALM = "master";
    private static final String ADMIN_CLI = "admin-cli";
    private static final String BOOTSTRAP_ADMIN = "admin";

    private final String baseUrl;
    private final String realm;
    private volatile boolean clientReady;

    public KeycloakTestUserToken(final String baseUrl, final String realm) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "'baseUrl' should not be null");
        this.realm = Objects.requireNonNull(realm, "'realm' should not be null");
    }

    public String bearerFor(final String username, final String password) {
        ensureTheClientExists();
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "password");
        form.add("client_id", E2E_CLIENT_ID);
        form.add("username", username);
        form.add("password", password);
        return "Bearer " + accessTokenOf(this.realm, form, "o token de " + username);
    }

    // Uma vez por execução: o client sobrevive no container, que é reaproveitado entre as classes.
    private void ensureTheClientExists() {
        if (this.clientReady) {
            return;
        }
        synchronized (this) {
            if (this.clientReady) {
                return;
            }
            createTheClient(adminToken());
            this.clientReady = true;
        }
    }

    private void createTheClient(final String adminToken) {
        final var body =
                """
                {"clientId":"%s","enabled":true,"publicClient":true,"directAccessGrantsEnabled":true,\
                "standardFlowEnabled":false,"serviceAccountsEnabled":false}"""
                        .formatted(E2E_CLIENT_ID);
        final var response = RestClient.create()
                .post()
                .uri("%s/admin/realms/%s/clients".formatted(this.baseUrl, this.realm))
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(status -> true, (request, ignored) -> {})
                .toBodilessEntity();
        // 409 é esperado quando outra execução já o criou e o container foi reaproveitado.
        if (!response.getStatusCode().is2xxSuccessful() && response.getStatusCode().value() != 409) {
            throw new AssertionError(
                    "não foi possível criar o client de teste: o Keycloak respondeu " + response.getStatusCode());
        }
    }

    private String adminToken() {
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "password");
        form.add("client_id", ADMIN_CLI);
        form.add("username", BOOTSTRAP_ADMIN);
        form.add("password", BOOTSTRAP_ADMIN);
        return accessTokenOf(MASTER_REALM, form, "o token do admin do container");
    }

    private String accessTokenOf(
            final String aRealm, final LinkedMultiValueMap<String, String> form, final String what) {
        final var response = RestClient.create()
                .post()
                .uri("%s/realms/%s/protocol/openid-connect/token".formatted(this.baseUrl, aRealm))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(status -> true, (request, ignored) -> {})
                .toEntity(Map.class);
        final var body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || body == null || body.get("access_token") == null) {
            throw new AssertionError(
                    "não foi possível obter %s: o Keycloak respondeu %s".formatted(what, response.getStatusCode()));
        }
        return String.valueOf(body.get("access_token"));
    }
}
