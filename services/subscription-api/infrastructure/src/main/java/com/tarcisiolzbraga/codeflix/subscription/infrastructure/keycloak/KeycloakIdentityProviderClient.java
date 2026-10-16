package com.tarcisiolzbraga.codeflix.subscription.infrastructure.keycloak;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.IdentityProviderGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.NewUser;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.User;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.UserId;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.InternalErrorException;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Name;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration.KeycloakAdminProperties;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration.KeycloakProperties;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// A conversa com a API administrativa do Keycloak.
public class KeycloakIdentityProviderClient implements IdentityProviderGateway {

    private static final Logger LOG = LoggerFactory.getLogger(KeycloakIdentityProviderClient.class);
    private static final String USERS = "/users";
    private static final String GROUPS = "/groups";

    private final RestClient restClient;
    private final KeycloakProperties keycloak;
    private final KeycloakAdminProperties admin;
    private final ClientCredentials credentials;

    public KeycloakIdentityProviderClient(
            final RestClient restClient,
            final KeycloakProperties keycloak,
            final KeycloakAdminProperties admin,
            final ClientCredentials credentials) {
        this.restClient = Objects.requireNonNull(restClient, "'restClient' should not be null");
        this.keycloak = Objects.requireNonNull(keycloak, "'keycloak' should not be null");
        this.admin = Objects.requireNonNull(admin, "'admin' should not be null");
        this.credentials = Objects.requireNonNull(credentials, "'credentials' should not be null");
    }

    // O usuário nasce com o e-mail como `username`: é por ele que a pessoa entra, e não por um nome
    // de usuário à parte. E nasce com o e-mail não conferido — quem o marca é a confirmação do
    // cadastro.
    @Override
    public UserId create(final NewUser aNewUser) {
        final var body = Map.of(
                "username", aNewUser.email().value(),
                "email", aNewUser.email().value(),
                "firstName", aNewUser.name().firstname(),
                "lastName", aNewUser.name().lastname(),
                "enabled", Boolean.TRUE,
                "emailVerified", Boolean.FALSE,
                "credentials",
                        List.of(Map.of("type", "password", "value", aNewUser.password(), "temporary", Boolean.FALSE)));

        try {
            final var response = post(USERS, body);
            final var location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
            return new UserId(idFrom(location));
        } catch (final HttpClientErrorException.Conflict exception) {
            // 409 é o e-mail já tomado. Sobe como conflito e não como erro interno: quem chamou
            // precisa saber que a pessoa já existe, e é o que leva o cadastro a só mandar validar.
            throw ConflictException.emailAlreadyTaken(aNewUser.email().value());
        } catch (final RestClientException exception) {
            throw failure("create the user", exception);
        }
    }

    @Override
    public Optional<User> userOfEmail(final Email anEmail) {
        try {
            final var found = this.restClient
                    .get()
                    .uri(uri(USERS) + "?email={email}&exact=true", anEmail.value())
                    .header(HttpHeaders.AUTHORIZATION, this.credentials.bearer())
                    .retrieve()
                    .body(List.class);
            if (found == null || found.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(userFrom((Map<?, ?>) found.getFirst()));
        } catch (final RestClientException exception) {
            throw failure("look the user up by email", exception);
        }
    }

    @Override
    public void markEmailVerified(final UserId anUserId) {
        try {
            this.restClient
                    .put()
                    .uri(uri(USERS) + "/{id}", anUserId.value())
                    .header(HttpHeaders.AUTHORIZATION, this.credentials.bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("emailVerified", Boolean.TRUE))
                    .retrieve()
                    .toBodilessEntity();
        } catch (final RestClientException exception) {
            throw failure("mark the email as verified", exception);
        }
    }

    @Override
    public void grantAccountAccess(final UserId anUserId) {
        join(anUserId, this.admin.accountsGroup());
    }

    @Override
    public void grantCatalogAccess(final UserId anUserId) {
        join(anUserId, this.admin.subscribersGroup());
    }

    @Override
    public void revokeCatalogAccess(final UserId anUserId) {
        leave(anUserId, this.admin.subscribersGroup());
    }

    // Pôr num grupo em que a pessoa já está devolve 204, e é o que permite chamar isto de dois
    // caminhos da confirmação sem conferir antes.
    private void join(final UserId anUserId, final String group) {
        final var groupId = groupIdOf(group);
        try {
            this.restClient
                    .put()
                    .uri(uri(USERS) + "/{id}/groups/{groupId}", anUserId.value(), groupId)
                    .header(HttpHeaders.AUTHORIZATION, this.credentials.bearer())
                    .retrieve()
                    .toBodilessEntity();
            LOG.debug("usuário {} entrou no grupo {}", anUserId.value(), group);
        } catch (final RestClientException exception) {
            throw failure("add the user to the '%s' group".formatted(group), exception);
        }
    }

    // Tirar de um grupo em que a pessoa não está também devolve 204: cancelar duas vezes, ou
    // cancelar assinatura que nunca chegou a valer, não é erro.
    private void leave(final UserId anUserId, final String group) {
        final var groupId = groupIdOf(group);
        try {
            this.restClient
                    .delete()
                    .uri(uri(USERS) + "/{id}/groups/{groupId}", anUserId.value(), groupId)
                    .header(HttpHeaders.AUTHORIZATION, this.credentials.bearer())
                    .retrieve()
                    .toBodilessEntity();
            LOG.debug("usuário {} saiu do grupo {}", anUserId.value(), group);
        } catch (final RestClientException exception) {
            throw failure("remove the user from the '%s' group".formatted(group), exception);
        }
    }

    // Resolve o id do grupo a cada chamada, sem guardar: o id nasce na importação do realm, e este
    // realm é descartável — guardado, ele apontaria para nada depois de recriar o container. O custo
    // é uma busca a mais numa operação que acontece poucas vezes na vida de um assinante.
    private String groupIdOf(final String name) {
        try {
            final var found = this.restClient
                    .get()
                    .uri(uri(GROUPS) + "?search={name}&exact=true", name)
                    .header(HttpHeaders.AUTHORIZATION, this.credentials.bearer())
                    .retrieve()
                    .body(List.class);
            if (found == null || found.isEmpty()) {
                throw InternalErrorException.with("the group '%s' does not exist in the realm".formatted(name));
            }
            return String.valueOf(((Map<?, ?>) found.getFirst()).get("id"));
        } catch (final RestClientException exception) {
            throw failure("look the '%s' group up".formatted(name), exception);
        }
    }

    private ResponseEntity<Void> post(final String path, final Map<String, Object> body) {
        return this.restClient
                .post()
                .uri(uri(path))
                .header(HttpHeaders.AUTHORIZATION, this.credentials.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private String uri(final String path) {
        return this.keycloak.adminRealmUri() + path;
    }

    private static User userFrom(final Map<?, ?> raw) {
        return new User(
                new UserId(String.valueOf(raw.get("id"))),
                new Name(String.valueOf(raw.get("firstName")), String.valueOf(raw.get("lastName"))),
                new Email(String.valueOf(raw.get("email"))),
                Boolean.TRUE.equals(raw.get("enabled")),
                Boolean.TRUE.equals(raw.get("emailVerified")));
    }

    // O Keycloak não devolve corpo na criação: o id vem no header Location, no fim do caminho.
    private static String idFrom(final String location) {
        if (location == null || location.isBlank()) {
            throw InternalErrorException.with("the identity provider created the user without a Location header");
        }
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private static InternalErrorException failure(final String what, final RestClientException cause) {
        return InternalErrorException.with("could not %s on the identity provider".formatted(what), cause);
    }
}
