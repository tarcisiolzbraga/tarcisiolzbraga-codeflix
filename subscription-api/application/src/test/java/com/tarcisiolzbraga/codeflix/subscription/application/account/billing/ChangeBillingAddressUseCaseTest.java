package com.tarcisiolzbraga.codeflix.subscription.application.account.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.Account;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChangeBillingAddressUseCaseTest {

    @Mock
    private AccountGateway accountGateway;

    @InjectMocks
    private DefaultChangeBillingAddressUseCase useCase;

    @Captor
    private ArgumentCaptor<Account> savedAccount;

    private static ChangeBillingAddressCommand command() {
        return ChangeBillingAddressCommand.with(AccountFixture.USER_ID, "01310930", "1578", "apto 42", "BR");
    }

    @Test
    void givenAnAccountWithoutBillingAddress_whenChangesIt_thenItIsStored() {
        final var stored = AccountFixture.storedAccount();
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(stored));
        when(accountGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase.execute(command());

        verify(accountGateway).save(savedAccount.capture());
        assertEquals(AccountFixture.billingAddress(), savedAccount.getValue().billingAddress());
    }

    @Test
    void givenAnAccount_whenChangesTheAddress_thenTheReturnedDataAlreadyShowsIt() {
        final var stored = AccountFixture.storedAccount();
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(stored));
        when(accountGateway.save(any())).thenAnswer(returnsFirstArg());

        final var actual = useCase.execute(command());

        assertEquals("01310930", actual.billingAddress().zipcode());
        assertEquals("apto 42", actual.billingAddress().complement());
    }

    // O complemento é o único opcional, e sem ele o endereço continua válido.
    @Test
    void givenNoComplement_whenChangesTheAddress_thenItIsAccepted() {
        final var stored = AccountFixture.storedAccount();
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(stored));
        when(accountGateway.save(any())).thenAnswer(returnsFirstArg());
        final var command = ChangeBillingAddressCommand.with(AccountFixture.USER_ID, "01310930", "1578", null, "BR");

        final var actual = useCase.execute(command);

        assertEquals("1578", actual.billingAddress().number());
    }

    // Mudar o endereço não mexe no resto da conta.
    @Test
    void givenAnAccount_whenChangesTheAddress_thenNothingElseChanges() {
        final var stored = AccountFixture.storedAccount();
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(stored));
        when(accountGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase.execute(command());

        verify(accountGateway).save(savedAccount.capture());
        assertEquals(AccountFixture.email(), savedAccount.getValue().email());
        assertEquals(AccountFixture.document(), savedAccount.getValue().document());
    }

    @Test
    void givenAnIncompleteAddress_whenChanges_thenRefusesAndSavesNothing() {
        final var stored = AccountFixture.storedAccount();
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(stored));
        final var command = ChangeBillingAddressCommand.with(AccountFixture.USER_ID, "01310930", "  ", null, "BR");

        final var actual = assertThrows(DomainException.class, () -> useCase.execute(command));

        assertEquals("'number' should not be empty", actual.getMessage());
        verify(accountGateway, never()).save(any());
    }

    @Test
    void givenAProviderIdWithNoAccount_whenChanges_thenAnswersNotFoundAndSavesNothing() {
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.empty());

        final var actual = assertThrows(NotFoundException.class, () -> useCase.execute(command()));

        assertEquals("there is no account for whoever is calling", actual.getMessage());
        verify(accountGateway, never()).save(any());
    }
}
