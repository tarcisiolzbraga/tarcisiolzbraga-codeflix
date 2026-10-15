package com.tarcisiolzbraga.codeflix.subscription.infrastructure.kafka;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.Account;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.IdentityProviderGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.NewUser;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.UserId;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.KeycloakTestToken;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration.KeycloakProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestClient;

// O caminho de chegada inteiro, com broker e provedor de verdade: mensagem no formato que o Debezium
// publica, consumida pelo listener, e o efeito conferido **no Keycloak** — em quais grupos a pessoa
// está.
//
// O único elo que fica de fora é o Debezium em si, que é configuração e não código: o conector está
// em ../provisioning/cdc/subscription-mysql-cdc.json e é verificado à mão, com a pilha de pé.
//
// A asserção pergunta ao Keycloak, e não a um mock: o que importa é o efeito no sistema de fora.
@IntegrationTest
class EventCdcListenerIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Value("${kafka.consumers.events.topics}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, String> testKafkaTemplate;

    @Autowired
    private AccountGateway accounts;

    @Autowired
    private IdentityProviderGateway identityProvider;

    @Autowired
    private KeycloakProperties keycloak;

    @Autowired
    private KeycloakTestToken token;

    @Test
    void givenASubscriptionCreatedEvent_whenConsumed_thenThePersonGetsTheCatalogInTheProvider() {
        final var account = anAccountWithAProviderUser();

        publish("SubscriptionCreated", account.id());

        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertEquals(List.of("subscribers"), groupsOf(account.userId())));
    }

    @Test
    void givenASubscriptionRenewedEvent_whenConsumed_thenTheCatalogIsGrantedAgain() {
        final var account = anAccountWithAProviderUser();

        publish("SubscriptionRenewed", account.id());

        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertEquals(List.of("subscribers"), groupsOf(account.userId())));
    }

    // O par que fecha a promessa: cancelar tira o acesso. Sem isto, assinatura cancelada continuaria
    // assistindo — era exatamente o furo desta fatia.
    @Test
    void givenASubscriptionCanceledEvent_whenConsumed_thenTheCatalogIsTakenAway() {
        final var account = anAccountWithAProviderUser();
        this.identityProvider.grantCatalogAccess(account.userId());

        publish("SubscriptionCanceled", account.id());

        await().atMost(TIMEOUT).untilAsserted(() -> assertEquals(List.of(), groupsOf(account.userId())));
    }

    // Incompleta não mexe no catálogo. Afirmar ausência pede paciência: o teste espera a mensagem
    // seguinte ser processada e só então confere que a primeira não mudou nada.
    @Test
    void givenASubscriptionIncompleteEvent_whenConsumed_thenTheCatalogIsLeftAlone() {
        final var account = anAccountWithAProviderUser();
        this.identityProvider.grantCatalogAccess(account.userId());
        final var marker = anAccountWithAProviderUser();

        publish("SubscriptionIncomplete", account.id());
        publish("SubscriptionCanceled", marker.id());

        await().atMost(TIMEOUT).untilAsserted(() -> assertEquals(List.of(), groupsOf(marker.userId())));
        assertEquals(List.of("subscribers"), groupsOf(account.userId()));
    }

    // Uma conta de verdade no banco e uma pessoa de verdade no provedor, ligadas pelo userId: é o
    // que o caso de uso atravessa para descobrir a quem conceder.
    //
    // A conta é montada aqui, e não por AccountFixture.accountWithUserId: a fixture traz e-mail
    // fixo, e um dos testes precisa de duas contas — a segunda batia no índice único de e-mail.
    private Account anAccountWithAProviderUser() {
        final var email = new Email("assinante-%s@codeflix.com.br".formatted(UUID.randomUUID()));
        final var userId =
                this.identityProvider.create(new NewUser(AccountFixture.name(), email, "uma-senha-boa"));
        final var account = Account.newAccount(
                AccountId.unique(), userId, email, AccountFixture.name(), AccountFixture.document());
        return this.accounts.save(account);
    }

    private void publish(final String eventType, final AccountId accountId) {
        this.testKafkaTemplate.send(this.topic, messageOf(eventType, accountId.getValue()));
    }

    private static String messageOf(final String eventType, final String accountId) {
        final var payload = """
                {"aggregateId":"%s","accountId":"%s","occurredOn":"2026-10-14T12:00:00Z"}"""
                .formatted(UUID.randomUUID(), accountId);
        return """
                {"schema":{"type":"struct"},"payload":{"before":null,"after":{"id":"%s",\
                "aggregate_type":"Subscription","aggregate_id":"%s","event_type":"%s",\
                "occurred_on":1760443200000000,"payload":%s},\
                "source":{"db":"subscription","table":"event"},"op":"c"}}"""
                .formatted(UUID.randomUUID(), UUID.randomUUID(), eventType, quoted(payload));
    }

    // A coluna é JSON no MySQL e o Debezium a entrega como texto: o payload chega escapado dentro da
    // mensagem, e é assim que o consumidor tem de saber lê-lo.
    private static String quoted(final String json) {
        return "\"" + json.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private List<String> groupsOf(final UserId userId) {
        final var body = RestClient.create()
                .get()
                .uri(this.keycloak.adminRealmUri() + "/users/{id}/groups", userId.value())
                .header(HttpHeaders.AUTHORIZATION, this.token.bearerOf("subscription-api-codeflix"))
                .retrieve()
                .body(List.class);
        return body == null
                ? List.of()
                : body.stream().map(it -> String.valueOf(((Map<?, ?>) it).get("name"))).toList();
    }
}
