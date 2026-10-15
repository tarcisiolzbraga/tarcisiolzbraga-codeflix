package com.tarcisiolzbraga.codeflix.subscription.application.account.access;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.IdentityProviderGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Os dois casos de uso que mexem no acesso ao catálogo. Juntos no mesmo teste de propósito: eles são
// o par que mantém a promessa — quem tem assinatura valendo assiste, quem cancelou não.
//
// Nenhum dos dois tem rota: quem os chama é o consumidor dos eventos.
@ExtendWith(MockitoExtension.class)
class CatalogAccessUseCaseTest {

    @Mock
    private AccountGateway accountGateway;

    @Mock
    private IdentityProviderGateway identityProviderGateway;

    @Test
    void givenAnAccountId_whenGrants_thenTheProviderIsToldAboutThePersonBehindIt() {
        final var stored = AccountFixture.storedAccount();
        when(accountGateway.accountOfId(any())).thenReturn(Optional.of(stored));

        grantUseCase().execute(stored.id().getValue());

        verify(identityProviderGateway).grantCatalogAccess(stored.userId());
    }

    @Test
    void givenAnAccountId_whenRevokes_thenTheProviderIsToldAboutThePersonBehindIt() {
        final var stored = AccountFixture.storedAccount();
        when(accountGateway.accountOfId(any())).thenReturn(Optional.of(stored));

        revokeUseCase().execute(stored.id().getValue());

        verify(identityProviderGateway).revokeCatalogAccess(stored.userId());
    }

    // Conceder não é revogar e vice-versa: trocar uma chamada pela outra é o tipo de erro que
    // libera catálogo para quem cancelou, e nenhum teste de rota pegaria.
    @Test
    void givenAnAccountId_whenGrants_thenNothingIsRevoked() {
        final var stored = AccountFixture.storedAccount();
        when(accountGateway.accountOfId(any())).thenReturn(Optional.of(stored));

        grantUseCase().execute(stored.id().getValue());

        verify(identityProviderGateway, never()).revokeCatalogAccess(any());
    }

    @Test
    void givenAnAccountId_whenRevokes_thenNothingIsGranted() {
        final var stored = AccountFixture.storedAccount();
        when(accountGateway.accountOfId(any())).thenReturn(Optional.of(stored));

        revokeUseCase().execute(stored.id().getValue());

        verify(identityProviderGateway, never()).grantCatalogAccess(any());
    }

    // Conta que não existe é erro, e precisa ser: o evento veio de uma assinatura com chave
    // estrangeira para a conta, então ela existia quando o fato aconteceu. Engolir isso em silêncio
    // deixaria acesso sem dono — e a mensagem na fila morta é o que faz alguém olhar.
    @Test
    void givenAnAccountThatDoesNotExist_whenGrants_thenRefusesAndTellsNothingToTheProvider() {
        final var unknown = UUID.randomUUID().toString();
        when(accountGateway.accountOfId(any())).thenReturn(Optional.empty());

        final var actual = assertThrows(NotFoundException.class, () -> grantUseCase().execute(unknown));

        assertTrue(actual.getMessage().contains("Account"));
        verify(identityProviderGateway, never()).grantCatalogAccess(any());
    }

    @Test
    void givenAnAccountThatDoesNotExist_whenRevokes_thenRefusesAndTellsNothingToTheProvider() {
        final var unknown = UUID.randomUUID().toString();
        when(accountGateway.accountOfId(any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> revokeUseCase().execute(unknown));

        verify(identityProviderGateway, never()).revokeCatalogAccess(any());
    }

    @Test
    void givenAMalformedAccountId_whenGrants_thenRefusesBeforeTouchingAnything() {
        assertThrows(RuntimeException.class, () -> grantUseCase().execute("nao-e-uuid"));

        verify(identityProviderGateway, never()).grantCatalogAccess(any());
    }

    private DefaultGrantCatalogAccessUseCase grantUseCase() {
        return new DefaultGrantCatalogAccessUseCase(accountGateway, identityProviderGateway);
    }

    private DefaultRevokeCatalogAccessUseCase revokeUseCase() {
        return new DefaultRevokeCatalogAccessUseCase(accountGateway, identityProviderGateway);
    }
}
