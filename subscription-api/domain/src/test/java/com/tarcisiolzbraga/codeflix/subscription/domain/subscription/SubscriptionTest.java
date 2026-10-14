package com.tarcisiolzbraga.codeflix.subscription.domain.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SubscriptionTest {

    @Test
    void givenAnAccountAndAPlan_whenSubscribes_thenItStartsInTrial() {
        final var actual = SubscriptionFixture.subscription();

        assertEquals(SubscriptionStatus.TRAILING, actual.status());
    }

    // O trial dura um mês, e é daí que sai a primeira cobrança.
    @Test
    void givenAnAccountAndAPlan_whenSubscribes_thenItIsDueInAMonth() {
        final var actual = SubscriptionFixture.subscription();

        assertEquals(LocalDate.now(ZoneOffset.UTC).plusMonths(1), actual.dueDate());
    }

    @Test
    void givenANewSubscription_whenCreated_thenItStartsAtVersionZeroAndWasNeverRenewed() {
        final var actual = SubscriptionFixture.subscription();

        assertEquals(0, actual.version());
        assertNull(actual.lastRenewDate());
        assertNull(actual.lastTransactionId());
    }

    @Test
    void givenANewSubscription_whenCreated_thenItRegisteredThatItWasCreated() {
        final var actual = SubscriptionFixture.subscription();

        assertEquals(1, actual.domainEvents().size());
        assertInstanceOf(SubscriptionCreated.class, actual.domainEvents().getFirst());
    }

    @Test
    void givenASubscriptionComingFromStorage_whenRebuilt_thenThereIsNoEvent() {
        final var actual = SubscriptionFixture.storedSubscription();

        assertTrue(actual.domainEvents().isEmpty());
    }

    @Test
    void givenNullAccount_whenSubscribes_thenThrowsDomainException() {
        final AccountId accountId = null;

        final var actual = assertThrows(
                DomainException.class,
                () -> Subscription.newSubscription(SubscriptionId.unique(), accountId, PlanId.unique()));

        assertEquals("'accountId' should not be null", actual.getMessage());
    }

    @Test
    void givenNullPlan_whenSubscribes_thenThrowsDomainException() {
        final PlanId planId = null;

        final var actual = assertThrows(
                DomainException.class,
                () -> Subscription.newSubscription(SubscriptionId.unique(), AccountId.unique(), planId));

        assertEquals("'planId' should not be null", actual.getMessage());
    }

    @Test
    void givenATrialSubscription_whenTheChargeSucceeds_thenItBecomesActive() {
        final var subscription = SubscriptionFixture.storedSubscription();

        subscription.execute(new SubscriptionCommand.Renew(SubscriptionFixture.TRANSACTION_ID));

        assertEquals(SubscriptionStatus.ACTIVE, subscription.status());
    }

    // Renovar empurra o vencimento um mês, que é o que faz a próxima cobrança acontecer no mês
    // seguinte e não na hora.
    @Test
    void givenASubscription_whenRenewed_thenTheDueDateMovesAMonthForward() {
        final var subscription =
                SubscriptionFixture.storedSubscription(SubscriptionStatus.ACTIVE, LocalDate.of(2026, 11, 14));

        subscription.execute(new SubscriptionCommand.Renew(SubscriptionFixture.TRANSACTION_ID));

        assertEquals(LocalDate.of(2026, 12, 14), subscription.dueDate());
    }

    @Test
    void givenASubscription_whenRenewed_thenItRecordsWhenAndWithWhichTransaction() {
        final var subscription = SubscriptionFixture.storedSubscription();

        subscription.execute(new SubscriptionCommand.Renew(SubscriptionFixture.TRANSACTION_ID));

        assertEquals(SubscriptionFixture.TRANSACTION_ID, subscription.lastTransactionId());
        assertTrue(subscription.lastRenewDate().isAfter(SubscriptionFixture.longAgo()));
    }

    @Test
    void givenASubscription_whenRenewed_thenItRegistersTheRenewal() {
        final var subscription = SubscriptionFixture.storedSubscription();

        subscription.execute(new SubscriptionCommand.Renew(SubscriptionFixture.TRANSACTION_ID));

        assertInstanceOf(SubscriptionRenewed.class, subscription.domainEvents().getFirst());
    }

    @Test
    void givenATrialSubscription_whenTheChargeFails_thenItBecomesIncomplete() {
        final var subscription = SubscriptionFixture.storedSubscription();

        subscription.execute(new SubscriptionCommand.MarkIncomplete("cartão recusado", "tx-0002"));

        assertEquals(SubscriptionStatus.INCOMPLETE, subscription.status());
        assertEquals("tx-0002", subscription.lastTransactionId());
    }

    // O vencimento NÃO anda quando a cobrança falha: é o que mantém a assinatura vencida e faz a
    // próxima tentativa acontecer.
    @Test
    void givenASubscription_whenTheChargeFails_thenTheDueDateDoesNotMove() {
        final var subscription =
                SubscriptionFixture.storedSubscription(SubscriptionStatus.TRAILING, LocalDate.of(2026, 11, 14));

        subscription.execute(new SubscriptionCommand.MarkIncomplete("cartão recusado", "tx-0002"));

        assertEquals(LocalDate.of(2026, 11, 14), subscription.dueDate());
    }

    @Test
    void givenASubscription_whenTheChargeFails_thenTheEventCarriesTheReason() {
        final var subscription = SubscriptionFixture.storedSubscription();

        subscription.execute(new SubscriptionCommand.MarkIncomplete("cartão recusado", "tx-0002"));

        final var actual = (SubscriptionIncomplete) subscription.domainEvents().getFirst();
        assertEquals("cartão recusado", actual.reason());
    }

    @Test
    void givenASubscription_whenCanceled_thenItBecomesCanceledAndSaysSo() {
        final var subscription = SubscriptionFixture.storedSubscription(SubscriptionStatus.ACTIVE);

        subscription.execute(new SubscriptionCommand.Cancel());

        assertEquals(SubscriptionStatus.CANCELED, subscription.status());
        assertInstanceOf(SubscriptionCanceled.class, subscription.domainEvents().getFirst());
    }

    // A recusa vem do estado, não do agregado: cancelado é terminal.
    @Test
    void givenACanceledSubscription_whenRenewed_thenTheStateRefuses() {
        final var subscription = SubscriptionFixture.storedSubscription(SubscriptionStatus.CANCELED);

        final var actual = assertThrows(
                DomainException.class,
                () -> subscription.execute(new SubscriptionCommand.Renew(SubscriptionFixture.TRANSACTION_ID)));

        assertEquals("a canceled subscription cannot become active", actual.getMessage());
    }

    @Test
    void givenACanceledSubscription_whenRenewalIsRefused_thenNothingAboutItChanged() {
        final var subscription =
                SubscriptionFixture.storedSubscription(SubscriptionStatus.CANCELED, LocalDate.of(2026, 11, 14));

        assertThrows(
                DomainException.class,
                () -> subscription.execute(new SubscriptionCommand.Renew(SubscriptionFixture.TRANSACTION_ID)));

        assertEquals(LocalDate.of(2026, 11, 14), subscription.dueDate());
        assertNull(subscription.lastTransactionId());
        assertTrue(subscription.domainEvents().isEmpty());
    }

    @Test
    void givenAStoredSubscription_whenItChanges_thenUpdatedAtMovesForwardAndTheVersionDoesNot() {
        final var subscription = SubscriptionFixture.storedSubscription();
        final var before = subscription.updatedAt();

        subscription.execute(new SubscriptionCommand.Renew(SubscriptionFixture.TRANSACTION_ID));

        assertTrue(subscription.updatedAt().isAfter(before));
        assertEquals(1, subscription.version());
    }

    @Test
    void givenAStoredSubscription_whenExecutesNothing_thenUpdatedAtIsUntouched() {
        final var subscription = SubscriptionFixture.storedSubscription();
        final var before = subscription.updatedAt();

        subscription.execute();

        assertEquals(before, subscription.updatedAt());
    }

    // O provedor recusa antes de abrir transação quando a comunicação falha, e aí não há id.
    // Exigir um obrigaria quem chama a inventar um valor, e id inventado no banco é pior que coluna
    // nula.
    @Test
    void givenAFailureWithNoTransaction_whenMarkedIncomplete_thenItIsAcceptedWithoutOne() {
        final var subscription = SubscriptionFixture.storedSubscription();

        subscription.execute(new SubscriptionCommand.MarkIncomplete("provedor fora do ar", null));

        assertEquals(SubscriptionStatus.INCOMPLETE, subscription.status());
        assertNull(subscription.lastTransactionId());
    }

    @Test
    void givenAnIncompleteWithoutAReason_whenCreated_thenTheCommandRefuses() {
        final var actual =
                assertThrows(DomainException.class, () -> new SubscriptionCommand.MarkIncomplete(" ", "tx-1"));

        assertEquals("'reason' should not be empty", actual.getMessage());
    }

    @Test
    void givenARenewWithoutATransaction_whenCreated_thenTheCommandRefuses() {
        final var actual = assertThrows(DomainException.class, () -> new SubscriptionCommand.Renew("  "));

        assertEquals("'transactionId' should not be empty", actual.getMessage());
    }
}
