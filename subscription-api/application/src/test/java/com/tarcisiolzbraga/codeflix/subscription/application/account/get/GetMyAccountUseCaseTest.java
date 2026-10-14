package com.tarcisiolzbraga.codeflix.subscription.application.account.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountCommand;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetMyAccountUseCaseTest {

    @Mock
    private AccountGateway accountGateway;

    @InjectMocks
    private DefaultGetMyAccountUseCase useCase;

    @Test
    void givenTheProviderIdOfSomeoneWithAnAccount_whenExecutes_thenReturnsTheirData() {
        final var stored = AccountFixture.storedAccount();
        when(accountGateway.accountOfUserId(AccountFixture.userId())).thenReturn(Optional.of(stored));

        final var actual = useCase.execute(AccountFixture.USER_ID);

        assertEquals(stored.id().getValue(), actual.id());
        assertEquals(AccountFixture.EMAIL, actual.email());
        assertEquals(AccountFixture.FIRSTNAME, actual.firstname());
        assertEquals(AccountFixture.CPF, actual.documentNumber());
        assertEquals("cpf", actual.documentType());
    }

    // Conta sem endereço de cobrança sai com nulo, não com um endereço de campos vazios.
    @Test
    void givenAnAccountWithoutBillingAddress_whenExecutes_thenTheAddressIsAbsent() {
        final var stored = AccountFixture.storedAccount();
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(stored));

        final var actual = useCase.execute(AccountFixture.USER_ID);

        assertNull(actual.billingAddress());
    }

    @Test
    void givenAnAccountWithBillingAddress_whenExecutes_thenItComesAlong() {
        final var stored = AccountFixture.storedAccount();
        stored.execute(new AccountCommand.ChangeBillingAddress(AccountFixture.billingAddress()));
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.of(stored));

        final var actual = useCase.execute(AccountFixture.USER_ID);

        assertEquals("01310930", actual.billingAddress().zipcode());
        assertEquals("BR", actual.billingAddress().country());
    }

    // Token válido de quem não tem conta aqui — um service account, por exemplo — não é erro de
    // autenticação: é não ter conta.
    @Test
    void givenAProviderIdWithNoAccount_whenExecutes_thenAnswersNotFound() {
        when(accountGateway.accountOfUserId(any())).thenReturn(Optional.empty());

        final var actual = assertThrows(NotFoundException.class, () -> useCase.execute(AccountFixture.USER_ID));

        assertEquals("there is no account for whoever is calling", actual.getMessage());
    }

    @Test
    void givenABlankProviderId_whenExecutes_thenRefuses() {
        final var actual = assertThrows(DomainException.class, () -> useCase.execute("  "));

        assertEquals("'userId' should not be empty", actual.getMessage());
    }
}
