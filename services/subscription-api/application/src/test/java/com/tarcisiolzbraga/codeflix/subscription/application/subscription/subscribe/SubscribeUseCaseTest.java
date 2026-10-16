package com.tarcisiolzbraga.codeflix.subscription.application.subscription.subscribe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionGateways;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscribeUseCaseTest {

    @Mock
    private AccountGateway accountGateway;

    @Mock
    private PlanGateway planGateway;

    @Mock
    private SubscriptionGateway subscriptionGateway;

    private DefaultSubscribeUseCase useCase() {
        return new DefaultSubscribeUseCase(
                new SubscriptionGateways(accountGateway, planGateway, subscriptionGateway));
    }

    private static SubscribeCommand commandFor(final Plan plan) {
        return new SubscribeCommand(AccountFixture.USER_ID, plan.id().getValue());
    }

    @Test
    void givenAnActivePlan_whenSubscribes_thenItStartsInTrial() {
        final var plan = PlanFixture.plan();
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(AccountFixture.storedAccount()));
        when(planGateway.planOfId(any())).thenReturn(Optional.of(plan));
        when(subscriptionGateway.latestSubscriptionOfAccount(any())).thenReturn(Optional.empty());
        when(subscriptionGateway.save(any())).thenAnswer(returnsFirstArg());

        final var actual = useCase().execute(commandFor(plan));

        assertEquals("trailing", actual.status());
        assertEquals(plan.id().getValue(), actual.planId());
    }

    // Plano fora de venda não se assina. A leitura do plano devolve o inativo de propósito — quem
    // administra precisa vê-lo —, então a recusa é aqui.
    @Test
    void givenAPlanOutOfSale_whenSubscribes_thenRefusesAndSavesNothing() {
        final var plan = PlanFixture.inactivePlan();
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(AccountFixture.storedAccount()));
        when(planGateway.planOfId(any())).thenReturn(Optional.of(plan));

        final var actual = assertThrows(DomainException.class, () -> useCase().execute(commandFor(plan)));

        assertEquals("this plan is not on sale", actual.getMessage());
        verify(subscriptionGateway, never()).save(any());
    }

    @Test
    void givenAPlanThatDoesNotExist_whenSubscribes_thenAnswersNotFound() {
        final var plan = PlanFixture.plan();
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(AccountFixture.storedAccount()));
        when(planGateway.planOfId(any())).thenReturn(Optional.empty());

        final var actual = assertThrows(NotFoundException.class, () -> useCase().execute(commandFor(plan)));

        assertEquals("Plan with id %s was not found".formatted(plan.id().getValue()), actual.getMessage());
    }

    @Test
    void givenAnAccountThatAlreadySubscribes_whenSubscribesAgain_thenRefusesWithConflict() {
        final var plan = PlanFixture.plan();
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(AccountFixture.storedAccount()));
        when(planGateway.planOfId(any())).thenReturn(Optional.of(plan));
        when(subscriptionGateway.latestSubscriptionOfAccount(any()))
                .thenReturn(Optional.of(SubscriptionFixture.storedSubscription(SubscriptionStatus.ACTIVE)));

        final var actual = assertThrows(ConflictException.class, () -> useCase().execute(commandFor(plan)));

        assertEquals("this account already has a subscription", actual.getMessage());
        verify(subscriptionGateway, never()).save(any());
    }

    // Cancelada não conta: reassinar depois de encerrar é legítimo.
    @Test
    void givenAnAccountWhoseSubscriptionWasCanceled_whenSubscribesAgain_thenItIsAllowed() {
        final var plan = PlanFixture.plan();
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(AccountFixture.storedAccount()));
        when(planGateway.planOfId(any())).thenReturn(Optional.of(plan));
        when(subscriptionGateway.latestSubscriptionOfAccount(any()))
                .thenReturn(Optional.of(SubscriptionFixture.storedSubscription(SubscriptionStatus.CANCELED)));
        when(subscriptionGateway.save(any())).thenAnswer(returnsFirstArg());

        final var actual = useCase().execute(commandFor(plan));

        assertEquals("trailing", actual.status());
    }

    @Test
    void givenSomeoneWithoutAnAccount_whenSubscribes_thenAnswersNotFound() {
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.empty());

        final var actual =
                assertThrows(NotFoundException.class, () -> useCase().execute(commandFor(PlanFixture.plan())));

        assertEquals("there is no account for whoever is calling", actual.getMessage());
    }

    @Test
    void givenAPlanIdThatIsNotUuid_whenSubscribes_thenRefuses() {
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(AccountFixture.storedAccount()));

        final var actual = assertThrows(
                DomainException.class,
                () -> useCase().execute(new SubscribeCommand(AccountFixture.USER_ID, "7")));

        assertEquals("'7' is not a valid PlanId", actual.getMessage());
    }
}
