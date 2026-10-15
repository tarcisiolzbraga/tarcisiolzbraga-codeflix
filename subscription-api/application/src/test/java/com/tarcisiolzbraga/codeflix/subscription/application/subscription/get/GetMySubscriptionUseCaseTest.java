package com.tarcisiolzbraga.codeflix.subscription.application.subscription.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionGateways;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
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
class GetMySubscriptionUseCaseTest {

    @Mock
    private AccountGateway accountGateway;

    @Mock
    private PlanGateway planGateway;

    @Mock
    private SubscriptionGateway subscriptionGateway;

    private DefaultGetMySubscriptionUseCase useCase() {
        return new DefaultGetMySubscriptionUseCase(
                new SubscriptionGateways(accountGateway, planGateway, subscriptionGateway));
    }

    @Test
    void givenSomeoneWhoSubscribes_whenAsksForIt_thenReturnsTheSubscription() {
        final var stored = SubscriptionFixture.storedSubscription(SubscriptionStatus.ACTIVE);
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(AccountFixture.storedAccount()));
        when(subscriptionGateway.latestSubscriptionOfAccount(any())).thenReturn(Optional.of(stored));

        final var actual = useCase().execute(AccountFixture.USER_ID);

        assertEquals(stored.id().getValue(), actual.id());
        assertEquals("active", actual.status());
        assertEquals(stored.dueDate(), actual.dueDate());
    }

    // Cancelada continua sendo devolvida: é o histórico de quem encerrou, e é por ele que a pessoa
    // sabe que não é mais assinante.
    @Test
    void givenACanceledSubscription_whenAsksForIt_thenItIsStillReturned() {
        final var stored = SubscriptionFixture.storedSubscription(SubscriptionStatus.CANCELED);
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(AccountFixture.storedAccount()));
        when(subscriptionGateway.latestSubscriptionOfAccount(any())).thenReturn(Optional.of(stored));

        final var actual = useCase().execute(AccountFixture.USER_ID);

        assertEquals("canceled", actual.status());
    }

    @Test
    void givenSomeoneWhoNeverSubscribed_whenAsksForIt_thenAnswersNotFound() {
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(AccountFixture.storedAccount()));
        when(subscriptionGateway.latestSubscriptionOfAccount(any())).thenReturn(Optional.empty());

        final var actual = assertThrows(NotFoundException.class, () -> useCase().execute(AccountFixture.USER_ID));

        assertEquals("there is no subscription for whoever is calling", actual.getMessage());
    }
}
