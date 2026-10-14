package com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionStatus;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.AccountJdbcGateway;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.PlanJdbcGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@IntegrationTest
class SubscriptionJdbcGatewayIT {

    @Autowired
    private SubscriptionJdbcGateway gateway;

    @Autowired
    private AccountJdbcGateway accounts;

    @Autowired
    private PlanJdbcGateway plans;

    @Test
    void givenANewSubscription_whenSavesIt_thenItIsFoundByItsAccount() {
        final var subscription = aSubscription();

        gateway.save(subscription);

        final var actual = gateway.latestSubscriptionOfAccount(subscription.accountId()).orElseThrow();
        assertEquals(subscription.id(), actual.id());
        assertEquals(SubscriptionStatus.TRAILING, actual.status());
        assertEquals(subscription.dueDate(), actual.dueDate());
    }

    @Test
    void givenANewSubscription_whenSavesIt_thenTheRowIsStoredAtVersionOne() {
        final var subscription = aSubscription();

        gateway.save(subscription);

        assertEquals(1, gateway.latestSubscriptionOfAccount(subscription.accountId()).orElseThrow().version());
    }

    // Nunca cobrada: as duas colunas de cobrança voltam nulas, e não com valor de enfeite.
    @Test
    void givenASubscriptionNeverCharged_whenReadsItBack_thenThereIsNoRenewalRecorded() {
        final var subscription = aSubscription();

        gateway.save(subscription);

        final var actual = gateway.latestSubscriptionOfAccount(subscription.accountId()).orElseThrow();
        assertNull(actual.lastRenewDate());
        assertNull(actual.lastTransactionId());
    }

    @Test
    void givenAnAccountWithNoSubscription_whenLooksItUp_thenNothingComesBack() {
        final var actual = gateway.latestSubscriptionOfAccount(AccountId.unique());

        assertTrue(actual.isEmpty());
    }

    @Test
    void givenAStoredSubscription_whenRenewedAndSaved_thenTheNewStateIsStored() {
        final var stored = savedSubscription();
        final var dueBefore = stored.dueDate();
        stored.execute(new SubscriptionCommand.Renew("tx-0001"));

        gateway.save(stored);

        final var actual = gateway.latestSubscriptionOfAccount(stored.accountId()).orElseThrow();
        assertEquals(SubscriptionStatus.ACTIVE, actual.status());
        assertEquals(dueBefore.plusMonths(1), actual.dueDate());
        assertEquals("tx-0001", actual.lastTransactionId());
        assertEquals(2, actual.version());
    }

    @Test
    void givenAStoredSubscription_whenMarkedIncompleteAndSaved_thenTheDueDateIsUnchanged() {
        final var stored = savedSubscription();
        final var dueBefore = stored.dueDate();
        stored.execute(new SubscriptionCommand.MarkIncomplete("cartão recusado", "tx-0002"));

        gateway.save(stored);

        final var actual = gateway.latestSubscriptionOfAccount(stored.accountId()).orElseThrow();
        assertEquals(SubscriptionStatus.INCOMPLETE, actual.status());
        assertEquals(dueBefore, actual.dueDate());
    }

    @Test
    void givenAStoredSubscription_whenUpdated_thenCreatedAtIsNotRewritten() {
        final var stored = savedSubscription();
        final var createdAt = stored.createdAt();
        stored.execute(new SubscriptionCommand.Cancel());

        gateway.save(stored);

        assertEquals(
                createdAt,
                gateway.latestSubscriptionOfAccount(stored.accountId()).orElseThrow().createdAt());
    }

    @Test
    void givenASubscriptionAtAVersionThatIsNoLongerCurrent_whenSavesIt_thenRefusesWithConflict() {
        final var stored = savedSubscription();
        stored.execute(new SubscriptionCommand.Cancel());
        gateway.save(stored);

        final var actual = assertThrows(ConflictException.class, () -> gateway.save(stored));

        assertTrue(actual.getMessage().contains("was changed by someone else"));
    }

    // A conta mais recente é a que vale: reassinar depois de cancelar cria linha nova.
    @Test
    void givenAnAccountThatSubscribedTwice_whenLooksItUp_thenTheNewestComesBack() {
        final var accountId = savedAccountId();
        final var planId = savedPlanId();
        final var first = Subscription.newSubscription(SubscriptionId.unique(), accountId, planId);
        gateway.save(first);
        first.execute(new SubscriptionCommand.Cancel());
        gateway.save(gateway.latestSubscriptionOfAccount(accountId).orElseThrow());
        final var second = Subscription.newSubscription(SubscriptionId.unique(), accountId, planId);

        gateway.save(second);

        assertEquals(second.id(), gateway.latestSubscriptionOfAccount(accountId).orElseThrow().id());
    }

    // A chave estrangeira é o que garante que não existe assinatura de conta que não existe, e o
    // banco é o único lugar que consegue afirmar isso sob concorrência.
    @Test
    void givenAnAccountThatDoesNotExist_whenSavesASubscription_thenTheForeignKeyRefuses() {
        final var orphan = Subscription.newSubscription(SubscriptionId.unique(), AccountId.unique(), savedPlanId());

        assertThrows(DataIntegrityViolationException.class, () -> gateway.save(orphan));
    }

    @Test
    void givenAPlanThatDoesNotExist_whenSavesASubscription_thenTheForeignKeyRefuses() {
        final var orphan =
                Subscription.newSubscription(SubscriptionId.unique(), savedAccountId(), PlanId.unique());

        assertThrows(DataIntegrityViolationException.class, () -> gateway.save(orphan));
    }

    private Subscription aSubscription() {
        return Subscription.newSubscription(SubscriptionId.unique(), savedAccountId(), savedPlanId());
    }

    // Grava e lê de volta, para o teste seguir com a versão que está no banco.
    private Subscription savedSubscription() {
        final var subscription = aSubscription();
        gateway.save(subscription);
        return gateway.latestSubscriptionOfAccount(subscription.accountId()).orElseThrow();
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
