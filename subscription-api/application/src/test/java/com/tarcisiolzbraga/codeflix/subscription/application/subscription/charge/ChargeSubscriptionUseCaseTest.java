package com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionGateways;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.Account;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountCommand;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.payment.Payment;
import com.tarcisiolzbraga.codeflix.subscription.domain.payment.PaymentGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.payment.Transaction;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.Subscription;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Os três caminhos do ciclo, decididos por "que dia é hoje" — e é por isso que o relógio é
// injetado: dá para testar a virada do prazo sem esperar o mês passar.
@ExtendWith(MockitoExtension.class)
class ChargeSubscriptionUseCaseTest {

    private static final LocalDate DUE = LocalDate.of(2026, 11, 14);
    private static final int TOLERANCE_DAYS = 2;
    private static final String CARD = "tok_abc123";

    @Mock
    private AccountGateway accountGateway;

    @Mock
    private PlanGateway planGateway;

    @Mock
    private SubscriptionGateway subscriptionGateway;

    @Mock
    private PaymentGateway paymentGateway;

    @Captor
    private ArgumentCaptor<Subscription> savedSubscription;

    @Captor
    private ArgumentCaptor<Payment> sentPayment;

    private DefaultChargeSubscriptionUseCase useCaseOn(final LocalDate today) {
        return new DefaultChargeSubscriptionUseCase(
                new SubscriptionGateways(accountGateway, planGateway, subscriptionGateway),
                paymentGateway,
                Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC),
                TOLERANCE_DAYS);
    }

    private static ChargeSubscriptionCommand command() {
        return new ChargeSubscriptionCommand(AccountFixture.USER_ID, CARD);
    }

    private Account anAccountWithAddress() {
        final var account = AccountFixture.storedAccount();
        account.execute(new AccountCommand.ChangeBillingAddress(AccountFixture.billingAddress()));
        return account;
    }

    private void givenAnAccountAndASubscription(final Account account, final Subscription subscription) {
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(account));
        when(subscriptionGateway.latestSubscriptionOfAccount(any())).thenReturn(Optional.of(subscription));
    }

    @Test
    void givenADueSubscriptionAndAnApprovedCharge_whenCharges_thenItBecomesActive() {
        final var subscription = SubscriptionFixture.storedSubscription(SubscriptionStatus.TRAILING, DUE);
        givenAnAccountAndASubscription(anAccountWithAddress(), subscription);
        when(planGateway.planOfId(any())).thenReturn(Optional.of(PlanFixture.plan()));
        when(paymentGateway.processPayment(any())).thenReturn(new Transaction.Succeeded("tx-ok"));
        when(subscriptionGateway.save(any())).thenAnswer(returnsFirstArg());

        final var actual = useCaseOn(DUE).execute(command());

        assertTrue(actual.charged());
        assertEquals("tx-ok", actual.transactionId());
        assertEquals("active", actual.subscription().status());
        assertNull(actual.reason());
    }

    @Test
    void givenAnApprovedCharge_whenCharges_thenTheDueDateMovesAMonthForward() {
        final var subscription = SubscriptionFixture.storedSubscription(SubscriptionStatus.TRAILING, DUE);
        givenAnAccountAndASubscription(anAccountWithAddress(), subscription);
        when(planGateway.planOfId(any())).thenReturn(Optional.of(PlanFixture.plan()));
        when(paymentGateway.processPayment(any())).thenReturn(new Transaction.Succeeded("tx-ok"));
        when(subscriptionGateway.save(any())).thenAnswer(returnsFirstArg());

        final var actual = useCaseOn(DUE).execute(command());

        assertEquals(DUE.plusMonths(1), actual.subscription().dueDate());
    }

    // O valor cobrado é o do plano, e o endereço é o da conta: é o que o provedor usa para analisar.
    @Test
    void givenADueSubscription_whenCharges_thenThePaymentCarriesThePlanPriceAndTheBillingAddress() {
        final var subscription = SubscriptionFixture.storedSubscription(SubscriptionStatus.TRAILING, DUE);
        givenAnAccountAndASubscription(anAccountWithAddress(), subscription);
        when(planGateway.planOfId(any())).thenReturn(Optional.of(PlanFixture.plan()));
        when(paymentGateway.processPayment(any())).thenReturn(new Transaction.Succeeded("tx-ok"));
        when(subscriptionGateway.save(any())).thenAnswer(returnsFirstArg());

        useCaseOn(DUE).execute(command());

        verify(paymentGateway).processPayment(sentPayment.capture());
        assertEquals(PlanFixture.price(), sentPayment.getValue().amount());
        assertEquals(AccountFixture.billingAddress(), sentPayment.getValue().billingAddress());
        assertEquals(CARD, sentPayment.getValue().creditCardToken());
    }

    @Test
    void givenARefusedChargeWithinTheTolerance_whenCharges_thenItBecomesIncomplete() {
        final var subscription = SubscriptionFixture.storedSubscription(SubscriptionStatus.TRAILING, DUE);
        givenAnAccountAndASubscription(anAccountWithAddress(), subscription);
        when(planGateway.planOfId(any())).thenReturn(Optional.of(PlanFixture.plan()));
        when(paymentGateway.processPayment(any()))
                .thenReturn(new Transaction.Failed("tx-no", "cartão recusado"));
        when(subscriptionGateway.save(any())).thenAnswer(returnsFirstArg());

        final var actual = useCaseOn(DUE.plusDays(TOLERANCE_DAYS)).execute(command());

        assertFalse(actual.charged());
        assertEquals("incomplete", actual.subscription().status());
        assertEquals("cartão recusado", actual.reason());
    }

    // A virada do prazo. Um dia depois do limite, a assinatura é cancelada — é o que impede uma
    // assinatura nunca paga de ficar viva para sempre.
    @Test
    void givenARefusedChargePastTheTolerance_whenCharges_thenItIsCanceled() {
        final var subscription = SubscriptionFixture.storedSubscription(SubscriptionStatus.INCOMPLETE, DUE);
        givenAnAccountAndASubscription(anAccountWithAddress(), subscription);
        when(planGateway.planOfId(any())).thenReturn(Optional.of(PlanFixture.plan()));
        when(paymentGateway.processPayment(any()))
                .thenReturn(new Transaction.Failed("tx-no", "saldo insuficiente"));
        when(subscriptionGateway.save(any())).thenAnswer(returnsFirstArg());

        final var actual = useCaseOn(DUE.plusDays(TOLERANCE_DAYS + 1)).execute(command());

        assertEquals("canceled", actual.subscription().status());
    }

    @Test
    void givenARefusedChargeWithNoTransaction_whenCharges_thenItStillBecomesIncomplete() {
        final var subscription = SubscriptionFixture.storedSubscription(SubscriptionStatus.TRAILING, DUE);
        givenAnAccountAndASubscription(anAccountWithAddress(), subscription);
        when(planGateway.planOfId(any())).thenReturn(Optional.of(PlanFixture.plan()));
        when(paymentGateway.processPayment(any()))
                .thenReturn(new Transaction.Failed(null, "provedor fora do ar"));
        when(subscriptionGateway.save(any())).thenAnswer(returnsFirstArg());

        final var actual = useCaseOn(DUE).execute(command());

        assertEquals("incomplete", actual.subscription().status());
        assertNull(actual.transactionId());
    }

    // Cobrar antes do vencimento seria cobrar duas vezes no mesmo mês.
    @Test
    void givenASubscriptionNotDueYet_whenCharges_thenNothingIsChargedAndNothingChanges() {
        final var subscription = SubscriptionFixture.storedSubscription(SubscriptionStatus.ACTIVE, DUE);
        givenAnAccountAndASubscription(anAccountWithAddress(), subscription);

        final var actual = useCaseOn(DUE.minusDays(1)).execute(command());

        assertFalse(actual.charged());
        assertEquals("active", actual.subscription().status());
        verify(paymentGateway, never()).processPayment(any());
        verify(subscriptionGateway, never()).save(any());
    }

    // Sem endereço o provedor não analisa a transação: é recusa de entrada, não tentativa perdida.
    @Test
    void givenAnAccountWithoutBillingAddress_whenCharges_thenRefusesBeforeTouchingTheProvider() {
        final var subscription = SubscriptionFixture.storedSubscription(SubscriptionStatus.TRAILING, DUE);
        givenAnAccountAndASubscription(AccountFixture.storedAccount(), subscription);

        final var actual = assertThrows(DomainException.class, () -> useCaseOn(DUE).execute(command()));

        assertEquals("the account has no billing address; set one before charging", actual.getMessage());
        verify(paymentGateway, never()).processPayment(any());
    }

    @Test
    void givenACanceledSubscription_whenChargeIsApproved_thenTheStateRefusesAndNothingIsSaved() {
        final var subscription = SubscriptionFixture.storedSubscription(SubscriptionStatus.CANCELED, DUE);
        givenAnAccountAndASubscription(anAccountWithAddress(), subscription);
        when(planGateway.planOfId(any())).thenReturn(Optional.of(PlanFixture.plan()));
        when(paymentGateway.processPayment(any())).thenReturn(new Transaction.Succeeded("tx-ok"));

        final var actual = assertThrows(DomainException.class, () -> useCaseOn(DUE).execute(command()));

        assertEquals("a canceled subscription cannot become active", actual.getMessage());
        verify(subscriptionGateway, never()).save(any());
    }

    @Test
    void givenAnApprovedCharge_whenCharges_thenTheSubscriptionIsSavedAlreadyRenewed() {
        final var subscription = SubscriptionFixture.storedSubscription(SubscriptionStatus.TRAILING, DUE);
        givenAnAccountAndASubscription(anAccountWithAddress(), subscription);
        when(planGateway.planOfId(any())).thenReturn(Optional.of(PlanFixture.plan()));
        when(paymentGateway.processPayment(any())).thenReturn(new Transaction.Succeeded("tx-ok"));
        when(subscriptionGateway.save(any())).thenAnswer(returnsFirstArg());

        useCaseOn(DUE).execute(command());

        verify(subscriptionGateway).save(savedSubscription.capture());
        assertEquals(SubscriptionStatus.ACTIVE, savedSubscription.getValue().status());
        assertEquals("tx-ok", savedSubscription.getValue().lastTransactionId());
    }
}
