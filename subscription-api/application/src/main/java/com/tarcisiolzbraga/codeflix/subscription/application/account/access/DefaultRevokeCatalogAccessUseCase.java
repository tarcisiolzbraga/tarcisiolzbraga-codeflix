package com.tarcisiolzbraga.codeflix.subscription.application.account.access;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.Account;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.IdentityProviderGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import java.util.Objects;

// Tira o acesso ao catálogo de quem teve a assinatura cancelada. É o que impede assinatura
// cancelada de continuar assistindo.
public class DefaultRevokeCatalogAccessUseCase extends RevokeCatalogAccessUseCase {

    private final AccountGateway accounts;
    private final IdentityProviderGateway identityProvider;

    public DefaultRevokeCatalogAccessUseCase(
            final AccountGateway accounts, final IdentityProviderGateway identityProvider) {
        this.accounts = Objects.requireNonNull(accounts, "'accounts' should not be null");
        this.identityProvider =
                Objects.requireNonNull(identityProvider, "'identityProvider' should not be null");
    }

    @Override
    public void execute(final String accountId) {
        this.identityProvider.revokeCatalogAccess(accountOf(accountId).userId());
    }

    private Account accountOf(final String accountId) {
        final var id = AccountId.from(accountId);
        return this.accounts.accountOfId(id).orElseThrow(() -> NotFoundException.with(Account.class, id));
    }
}
