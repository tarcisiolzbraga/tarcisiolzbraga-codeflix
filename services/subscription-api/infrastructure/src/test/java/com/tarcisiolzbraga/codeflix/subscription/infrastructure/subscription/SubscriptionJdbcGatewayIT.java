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
import java.time.LocalDate;
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

    // A consulta da varredura, contra o banco: é onde o SQL do filtro, da ordem e do limite vale.
    @Test
    void givenSubscriptionsDueAndNotDue_whenAsksForTheChargeable_thenOnlyTheDueComeBack() {
        final var accountId = savedAccountId();
        final var planId = savedPlanId();
        final var due = savedWithDueDate(accountId, planId, LocalDate.now().minusDays(1));
        savedWithDueDate(savedUniqueAccountId(), planId, LocalDate.now().plusDays(1));

        final var actual = gateway.chargeableSubscriptionsDueBy(LocalDate.now(), 10, 0);

        assertEquals(1, actual.size());
        assertEquals(due.id(), actual.getFirst().id());
    }

    // Cancelada nunca volta a ser cobrável, e a consulta a deixa de fora. Trazê-la só para o
    // chamador descartar faria cada página render menos trabalho útil.
    @Test
    void givenACanceledSubscriptionLongOverdue_whenAsksForTheChargeable_thenItIsNotThere() {
        final var accountId = savedAccountId();
        final var stored = savedWithDueDate(accountId, savedPlanId(), LocalDate.now().minusDays(30));
        final var reread = gateway.latestSubscriptionOfAccount(accountId).orElseThrow();
        reread.execute(new SubscriptionCommand.Cancel());
        gateway.save(reread);

        final var actual = gateway.chargeableSubscriptionsDueBy(LocalDate.now(), 10, 0);

        assertTrue(
                actual.stream().noneMatch(it -> it.id().equals(stored.id())),
                "a cancelada não deveria estar entre as cobráveis");
    }

    // Do vencimento mais antigo para o mais novo: quem está vencido há mais tempo é cobrado
    // primeiro, e é quem está mais perto de ser cancelado pelo fim da tolerância.
    @Test
    void givenSeveralOverdue_whenAsksForTheChargeable_thenTheOldestDueComesFirst() {
        final var planId = savedPlanId();
        savedWithDueDate(savedUniqueAccountId(), planId, LocalDate.now().minusDays(1));
        final var oldest = savedWithDueDate(savedUniqueAccountId(), planId, LocalDate.now().minusDays(10));
        savedWithDueDate(savedUniqueAccountId(), planId, LocalDate.now().minusDays(5));

        final var actual = gateway.chargeableSubscriptionsDueBy(LocalDate.now(), 10, 0);

        assertEquals(3, actual.size());
        assertEquals(oldest.id(), actual.getFirst().id());
    }

    @Test
    void givenMoreOverdueThanTheLimit_whenAsksWithAnOffset_thenThePagesDoNotOverlap() {
        final var planId = savedPlanId();
        for (var i = 0; i < 5; i++) {
            savedWithDueDate(savedUniqueAccountId(), planId, LocalDate.now().minusDays(i + 1L));
        }

        final var first = gateway.chargeableSubscriptionsDueBy(LocalDate.now(), 2, 0);
        final var second = gateway.chargeableSubscriptionsDueBy(LocalDate.now(), 2, 2);

        assertEquals(2, first.size());
        assertEquals(2, second.size());
        assertTrue(
                first.stream().noneMatch(a -> second.stream().anyMatch(b -> b.id().equals(a.id()))),
                "as duas páginas não deveriam repetir ninguém");
    }

    // Conta sorteada: estes testes criam várias, e a conta da fixture tem e-mail e id de provedor
    // fixos, com índice único nos dois.
    private AccountId savedUniqueAccountId() {
        final var account = AccountFixture.uniqueAccount();
        this.accounts.save(account);
        return account.id();
    }

    private Subscription savedWithDueDate(
            final AccountId accountId, final PlanId planId, final LocalDate dueDate) {
        final var subscription = Subscription.with(
                SubscriptionId.unique(),
                0,
                accountId,
                planId,
                SubscriptionStatus.TRAILING,
                dueDate,
                null,
                null,
                java.time.Instant.now(),
                java.time.Instant.now());
        gateway.save(subscription);
        return subscription;
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
