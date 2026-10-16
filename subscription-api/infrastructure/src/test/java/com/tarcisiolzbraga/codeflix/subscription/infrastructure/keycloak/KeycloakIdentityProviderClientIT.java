package com.tarcisiolzbraga.codeflix.subscription.infrastructure.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.IdentityProviderGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.NewUser;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.UserId;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.InternalErrorException;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.KeycloakTestToken;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration.KeycloakProperties;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

// Contra o Keycloak de verdade, com o realm versionado do monorepo. Nada aqui é simulado: é a única
// forma de saber que as roles do client bastam e que os caminhos da API administrativa estão certos.
//
// Cada teste usa um e-mail próprio: o realm não é limpo entre testes como o banco é, porque o
// container é um só para a execução toda.
@IntegrationTest
class KeycloakIdentityProviderClientIT {

    // O corpo que o Keycloak devolve em /users/{id}/groups: uma lista de objetos, e só o nome
    // interessa aqui.
    private static final ParameterizedTypeReference<List<Map<String, Object>>> GRUPOS =
            new ParameterizedTypeReference<>() {};

    private static final String PASSWORD = "uma-senha-boa";

    @Autowired
    private IdentityProviderGateway gateway;

    @Autowired
    private KeycloakProperties keycloak;

    @Autowired
    private KeycloakTestToken token;

    @Test
    void givenANewUser_whenCreatesIt_thenTheProviderGivesBackAnId() {
        final var newUser = newUserWith(anEmail());

        final var actual = gateway.create(newUser);

        assertNotNull(actual);
        assertFalse(actual.value().isBlank());
    }

    @Test
    void givenACreatedUser_whenLooksItUpByEmail_thenFindsItWithWhatWasSent() {
        final var email = anEmail();
        final var userId = gateway.create(newUserWith(email));

        final var actual = gateway.userOfEmail(email).orElseThrow();

        assertEquals(userId, actual.userId());
        assertEquals(email, actual.email());
        assertEquals(AccountFixture.name(), actual.name());
    }

    // Nasce habilitado e com o e-mail não conferido: quem o marca é a confirmação do cadastro.
    @Test
    void givenACreatedUser_whenLooksItUp_thenTheEmailIsNotVerifiedYet() {
        final var email = anEmail();
        gateway.create(newUserWith(email));

        final var actual = gateway.userOfEmail(email).orElseThrow();

        assertTrue(actual.enabled());
        assertFalse(actual.emailVerified());
    }

    @Test
    void givenNoSuchEmail_whenLooksItUp_thenNothingComesBack() {
        final var email = anEmail();

        final var actual = gateway.userOfEmail(email);

        assertTrue(actual.isEmpty());
    }

    // O e-mail é por onde a pessoa entra: o provedor recusa o segundo com 409, e isso tem de chegar
    // como conflito, não como erro interno — é o que leva o cadastro a só mandar validar.
    @Test
    void givenAnEmailAlreadyTaken_whenCreatesAgain_thenItComesBackAsConflict() {
        final var email = anEmail();
        gateway.create(newUserWith(email));

        final var actual = assertThrows(ConflictException.class, () -> gateway.create(newUserWith(email)));

        assertEquals("the email %s is already taken".formatted(email.value()), actual.getMessage());
    }

    @Test
    void givenACreatedUser_whenMarksTheEmailVerified_thenTheProviderSaysItIsVerified() {
        final var email = anEmail();
        final var userId = gateway.create(newUserWith(email));

        gateway.markEmailVerified(userId);

        assertTrue(gateway.userOfEmail(email).orElseThrow().emailVerified());
    }

    // O PUT do Keycloak recebe a representação do usuário, e mandar um campo só poderia apagar o
    // resto. Este teste existe para provar que não apaga: depois de marcar, o e-mail e o nome
    // continuam lá.
    @Test
    void givenACreatedUser_whenMarksTheEmailVerified_thenNothingElseIsWipedOut() {
        final var email = anEmail();
        final var userId = gateway.create(newUserWith(email));

        gateway.markEmailVerified(userId);

        final var actual = gateway.userOfEmail(email).orElseThrow();
        assertEquals(email, actual.email());
        assertEquals(AccountFixture.name(), actual.name());
        assertTrue(actual.enabled());
    }

    // O grupo é procurado pelo nome a cada chamada: o id dele nasce na importação do realm, e
    // guardá-lo faria a chamada falhar depois de recriar o container.
    //
    // A asserção pergunta ao próprio Keycloak em quais grupos o usuário está: o gateway não expõe
    // isso, e não vai expor só para o teste — o que importa afirmar é o efeito no sistema de fora.
    @Test
    void givenACreatedUser_whenGrantsAccountAccess_thenTheProviderPutsItInTheAccountsGroup() {
        final var userId = gateway.create(newUserWith(anEmail()));

        gateway.grantAccountAccess(userId);

        assertEquals(List.of("accounts"), groupsOf(userId));
    }

    // Os dois acessos são independentes: ter conta não dá catálogo, e é exatamente isso que impede
    // quem só confirmou um e-mail de assistir sem pagar.
    @Test
    void givenAUserWithAccountAccess_whenGrantsCatalogAccess_thenItIsInBothGroups() {
        final var userId = gateway.create(newUserWith(anEmail()));
        gateway.grantAccountAccess(userId);

        gateway.grantCatalogAccess(userId);

        assertEquals(List.of("accounts", "subscribers"), groupsOf(userId).stream().sorted().toList());
    }

    @Test
    void givenAUserInBothGroups_whenRevokesCatalogAccess_thenOnlyTheAccountAccessRemains() {
        final var userId = gateway.create(newUserWith(anEmail()));
        gateway.grantAccountAccess(userId);
        gateway.grantCatalogAccess(userId);

        gateway.revokeCatalogAccess(userId);

        assertEquals(List.of("accounts"), groupsOf(userId));
    }

    // Cancelar duas vezes, ou cancelar assinatura que nunca chegou a valer, não é erro: o Keycloak
    // responde 204 ao tirar de um grupo em que a pessoa não está.
    @Test
    void givenAUserWithoutCatalogAccess_whenRevokesIt_thenNothingHappensAndItDoesNotFail() {
        final var userId = gateway.create(newUserWith(anEmail()));
        gateway.grantAccountAccess(userId);

        gateway.revokeCatalogAccess(userId);

        assertEquals(List.of("accounts"), groupsOf(userId));
    }

    // Conceder duas vezes também não é erro, e é o que permite a confirmação repetida consertar uma
    // interrupção sem conferir antes.
    @Test
    void givenAUserAlreadyInTheGroup_whenGrantsAccountAccessAgain_thenItStaysInItOnce() {
        final var userId = gateway.create(newUserWith(anEmail()));
        gateway.grantAccountAccess(userId);

        gateway.grantAccountAccess(userId);

        assertEquals(List.of("accounts"), groupsOf(userId));
    }

    @Test
    void givenACreatedUser_whenNotAddedToAnyGroup_thenItIsInNone() {
        final var userId = gateway.create(newUserWith(anEmail()));

        final var actual = groupsOf(userId);

        assertTrue(actual.isEmpty());
    }

    @Test
    void givenNoSuchUser_whenGrantsAccountAccess_thenTheFailureSurfaces() {
        final var unknown = new UserId(UUID.randomUUID().toString());

        final var actual = assertThrows(InternalErrorException.class, () -> gateway.grantAccountAccess(unknown));

        assertTrue(actual.getMessage().contains("add the user to the 'accounts' group"));
    }

    @Test
    void givenNoSuchUser_whenRevokesCatalogAccess_thenTheFailureSurfaces() {
        final var unknown = new UserId(UUID.randomUUID().toString());

        final var actual = assertThrows(InternalErrorException.class, () -> gateway.revokeCatalogAccess(unknown));

        assertTrue(actual.getMessage().contains("remove the user from the 'subscribers' group"));
    }

    private List<String> groupsOf(final UserId userId) {
        // ParameterizedTypeReference, e não List.class: a segunda devolve List cru, e aí o stream
        // perde o tipo e o compilador avisa de conversão não verificada. Dizendo o tipo aqui, o
        // cast de cada item desaparece junto.
        final var body = RestClient.create()
                .get()
                .uri(this.keycloak.adminRealmUri() + "/users/{id}/groups", userId.value())
                .header(HttpHeaders.AUTHORIZATION, this.token.bearerOf("subscription-api-codeflix"))
                .retrieve()
                .body(GRUPOS);
        return body == null ? List.of() : body.stream().map(it -> String.valueOf(it.get("name"))).toList();
    }

    private static NewUser newUserWith(final Email email) {
        return new NewUser(AccountFixture.name(), email, PASSWORD);
    }

    // E-mail diferente por teste: o realm não é esvaziado entre eles.
    private static Email anEmail() {
        return new Email("assinante-%s@codeflix.com.br".formatted(UUID.randomUUID()));
    }
}
