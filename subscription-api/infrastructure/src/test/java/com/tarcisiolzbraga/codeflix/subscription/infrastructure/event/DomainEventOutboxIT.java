package com.tarcisiolzbraga.codeflix.subscription.infrastructure.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.Subscription;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionCommand;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionId;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.AccountJdbcGateway;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.PlanJdbcGateway;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.SubscriptionJdbcGateway;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

// A fila de saída, pelo caminho por onde ela é usada de verdade: salvando agregados.
//
// O que importa afirmar não é que um INSERT funciona — é que o evento e o agregado vão **juntos**.
// Por isso o teste lê a tabela direto, em vez de passar por um gateway de evento que não existe: o
// que está em jogo é o efeito no banco.
@IntegrationTest
class DomainEventOutboxIT {

    @Autowired
    private SubscriptionJdbcGateway subscriptions;

    @Autowired
    private AccountJdbcGateway accounts;

    @Autowired
    private PlanJdbcGateway plans;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void givenANewSubscription_whenSaved_thenTheOutboxHasTheCreatedEvent() {
        final var subscription = aSubscription();

        this.subscriptions.save(subscription);

        final var events = eventsOf(subscription.id().getValue());
        assertEquals(1, events.size());
        assertEquals("SubscriptionCreated", events.getFirst().get("event_type"));
        assertEquals("Subscription", events.getFirst().get("aggregate_type"));
    }

    // De quem é a assinatura tem de estar no payload: é o único jeito de quem consome saber a quem
    // conceder o catálogo sem ler a assinatura de volta.
    @Test
    void givenANewSubscription_whenSaved_thenThePayloadCarriesTheAccount() {
        final var subscription = aSubscription();

        this.subscriptions.save(subscription);

        final var payload = String.valueOf(eventsOf(subscription.id().getValue()).getFirst().get("payload"));
        assertTrue(
                payload.contains(subscription.accountId().getValue()),
                "o payload deveria trazer a conta, e trouxe: " + payload);
    }

    @Test
    void givenASubscriptionCharged_whenSaved_thenTheOutboxHasTheRenewedEvent() {
        final var stored = savedSubscription();
        stored.execute(new SubscriptionCommand.Renew("tx-1"));

        this.subscriptions.save(stored);

        assertEquals(List.of("SubscriptionCreated", "SubscriptionRenewed"), eventTypesOf(stored.id().getValue()));
    }

    @Test
    void givenASubscriptionCanceled_whenSaved_thenTheOutboxHasTheCanceledEvent() {
        final var stored = savedSubscription();
        stored.execute(new SubscriptionCommand.Cancel());

        this.subscriptions.save(stored);

        assertEquals(List.of("SubscriptionCreated", "SubscriptionCanceled"), eventTypesOf(stored.id().getValue()));
    }

    // Escrita do agregado que falha não deixa evento. Isto prova a **ordem**, não a transação: o
    // ConflictException estoura no UPDATE, antes de o evento ser gravado. Quem prova a transação é o
    // OutboxTransactionIT — descobri a diferença tirando o @Transactional e vendo este teste passar
    // igual.
    @Test
    void givenASubscriptionThatLostTheLock_whenSaveFails_thenNoEventIsLeftBehind() {
        final var stored = savedSubscription();
        stored.execute(new SubscriptionCommand.Cancel());
        this.subscriptions.save(stored);
        final var beforeTheFailure = eventTypesOf(stored.id().getValue());

        assertThrows(ConflictException.class, () -> this.subscriptions.save(stored));

        assertEquals(beforeTheFailure, eventTypesOf(stored.id().getValue()));
    }

    // Entregar é esquecer: salvar de novo não grava o mesmo evento outra vez.
    @Test
    void givenASubscriptionSavedTwiceWithoutNewEvents_whenSaved_thenTheOutboxDoesNotGrow() {
        final var subscription = aSubscription();
        this.subscriptions.save(subscription);

        final var reread =
                this.subscriptions.latestSubscriptionOfAccount(subscription.accountId()).orElseThrow();
        this.subscriptions.save(reread);

        assertEquals(List.of("SubscriptionCreated"), eventTypesOf(subscription.id().getValue()));
    }

    @Test
    void givenANewAccount_whenSaved_thenTheOutboxHasTheAccountCreatedEvent() {
        final var account = AccountFixture.account();

        this.accounts.save(account);

        assertEquals(List.of("AccountCreated"), eventTypesOf(account.id().getValue()));
    }

    private List<Map<String, Object>> eventsOf(final String aggregateId) {
        return this.jdbcClient
                .sql("SELECT * FROM event WHERE aggregate_id = :id ORDER BY occurred_on, event_type")
                .param("id", aggregateId)
                .query()
                .listOfRows();
    }

    private List<String> eventTypesOf(final String aggregateId) {
        return eventsOf(aggregateId).stream()
                .map(row -> String.valueOf(row.get("event_type")))
                .toList();
    }

    private Subscription aSubscription() {
        return Subscription.newSubscription(SubscriptionId.unique(), savedAccountId(), savedPlanId());
    }

    private Subscription savedSubscription() {
        final var subscription = aSubscription();
        this.subscriptions.save(subscription);
        return this.subscriptions
                .latestSubscriptionOfAccount(subscription.accountId())
                .orElseThrow();
    }

    private AccountId savedAccountId() {
        final var account = AccountFixture.account();
        this.accounts.save(account);
        return account.id();
    }

    private PlanId savedPlanId() {
        final var plan = PlanFixture.plan();
        this.plans.save(plan);
        return plan.id();
    }
}
