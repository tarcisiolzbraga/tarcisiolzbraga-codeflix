package com.tarcisiolzbraga.codeflix.subscription.application.account.paymentmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.payment.PaymentGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.payment.PaymentMethod;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorePaymentMethodUseCaseTest {

    private static final String CARD = "tok_visa_4242";
    private static final PaymentMethod STORED = new PaymentMethod("pm_stored_1");

    @Mock
    private AccountGateway accountGateway;

    @Mock
    private PaymentGateway payments;

    @InjectMocks
    private DefaultStorePaymentMethodUseCase useCase;

    @Captor
    private ArgumentCaptor<com.tarcisiolzbraga.codeflix.subscription.domain.account.Account> saved;

    @Test
    void givenACardToken_whenStores_thenTheProviderKeepsTheCardAndTheAccountKeepsTheReference() {
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(AccountFixture.storedAccount()));
        when(accountGateway.save(any())).thenAnswer(returnsFirstArg());
        when(payments.storeCard(CARD)).thenReturn(STORED);

        final var actual = useCase.execute(new StorePaymentMethodCommand(AccountFixture.USER_ID, CARD));

        verify(payments).storeCard(CARD);
        verify(accountGateway).save(saved.capture());
        assertEquals(STORED, saved.getValue().paymentMethod());
        assertTrue(actual.hasPaymentMethod());
    }

    // A conta só é tocada depois de o provedor confirmar: na ordem inversa, uma falha dele deixaria
    // no banco uma referência que não existe do lado de lá.
    @Test
    void givenAProviderThatFails_whenStores_thenTheAccountIsNotSaved() {
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(AccountFixture.storedAccount()));
        when(payments.storeCard(any())).thenThrow(new IllegalStateException("provedor fora do ar"));

        assertThrows(
                IllegalStateException.class,
                () -> useCase.execute(new StorePaymentMethodCommand(AccountFixture.USER_ID, CARD)));

        verify(accountGateway, never()).save(any());
    }

    @Test
    void givenNoCardToken_whenStores_thenRefusesBeforeTouchingTheProvider() {
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(AccountFixture.storedAccount()));

        final var actual = assertThrows(
                DomainException.class,
                () -> useCase.execute(new StorePaymentMethodCommand(AccountFixture.USER_ID, "  ")));

        assertEquals("'creditCardToken' should not be empty", actual.getMessage());
        verify(payments, never()).storeCard(any());
    }

    @Test
    void givenSomeoneWithoutAnAccount_whenStores_thenAnswersNotFound() {
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> useCase.execute(new StorePaymentMethodCommand("nao-tem-conta", CARD)));

        verify(payments, never()).storeCard(any());
    }

    // Trocar de cartão substitui o guardado: a conta tem um meio de pagamento, não uma coleção.
    @Test
    void givenAnAccountThatAlreadyHasOne_whenStoresAnother_thenItReplacesTheReference() {
        final var account = AccountFixture.storedAccount();
        account.execute(new com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountCommand
                .ChangePaymentMethod(new PaymentMethod("pm_antigo")));
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(account));
        when(accountGateway.save(any())).thenAnswer(returnsFirstArg());
        when(payments.storeCard(CARD)).thenReturn(STORED);

        useCase.execute(new StorePaymentMethodCommand(AccountFixture.USER_ID, CARD));

        verify(accountGateway).save(saved.capture());
        assertEquals(STORED, saved.getValue().paymentMethod());
    }
}
