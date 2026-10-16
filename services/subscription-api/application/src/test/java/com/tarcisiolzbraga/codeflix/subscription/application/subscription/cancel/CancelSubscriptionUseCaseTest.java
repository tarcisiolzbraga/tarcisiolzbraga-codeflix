package com.tarcisiolzbraga.codeflix.subscription.application.subscription.cancel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionGateways;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.Subscription;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelSubscriptionUseCaseTest {

    @Mock
    private AccountGateway accountGateway;

    @Mock
    private PlanGateway planGateway;

    @Mock
    private SubscriptionGateway subscriptionGateway;

    @Captor
    private ArgumentCaptor<Subscription> saved;

    private DefaultCancelSubscriptionUseCase useCase() {
        return new DefaultCancelSubscriptionUseCase(
                new SubscriptionGateways(accountGateway, planGateway, subscriptionGateway));
    }

    private void given(final SubscriptionStatus status) {
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(AccountFixture.storedAccount()));
        when(subscriptionGateway.latestSubscriptionOfAccount(any()))
                .thenReturn(Optional.of(SubscriptionFixture.storedSubscription(status)));
        when(subscriptionGateway.save(any())).thenAnswer(returnsFirstArg());
    }

    @Test
    void givenAnActiveSubscription_whenCanceled_thenItBecomesCanceled() {
        given(SubscriptionStatus.ACTIVE);

        final var actual = useCase().execute(AccountFixture.USER_ID);

        assertEquals("canceled", actual.status());
        verify(subscriptionGateway).save(saved.capture());
        assertEquals(SubscriptionStatus.CANCELED, saved.getValue().status());
    }

    @Test
    void givenATrialSubscription_whenCanceled_thenItBecomesCanceled() {
        given(SubscriptionStatus.TRAILING);

        final var actual = useCase().execute(AccountFixture.USER_ID);

        assertEquals("canceled", actual.status());
    }

    // Cancelar o que já está cancelado não é erro: quem pede duas vezes não precisa ver um erro
    // para saber que está cancelado.
    @Test
    void givenAnAlreadyCanceledSubscription_whenCanceledAgain_thenItStaysCanceled() {
        given(SubscriptionStatus.CANCELED);

        final var actual = useCase().execute(AccountFixture.USER_ID);

        assertEquals("canceled", actual.status());
    }

    @Test
    void givenSomeoneWhoNeverSubscribed_whenCancels_thenAnswersNotFound() {
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(AccountFixture.storedAccount()));
        when(subscriptionGateway.latestSubscriptionOfAccount(any())).thenReturn(Optional.empty());

        final var actual = assertThrows(NotFoundException.class, () -> useCase().execute(AccountFixture.USER_ID));

        assertEquals("there is no subscription for whoever is calling", actual.getMessage());
    }
}
