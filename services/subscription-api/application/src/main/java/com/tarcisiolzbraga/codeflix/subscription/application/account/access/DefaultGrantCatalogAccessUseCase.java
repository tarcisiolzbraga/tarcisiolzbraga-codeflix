package com.tarcisiolzbraga.codeflix.subscription.application.account.access;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.Account;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.IdentityProviderGateway;
import java.util.Objects;

// Dá acesso ao catálogo a quem passou a ter assinatura valendo.
//
// Chamado pelo consumidor dos eventos, não por rota: ninguém pede este acesso, ele é consequência
// de uma assinatura começar ou ser renovada.
public class DefaultGrantCatalogAccessUseCase extends GrantCatalogAccessUseCase {

    private final AccountGateway accounts;
    private final IdentityProviderGateway identityProvider;

    public DefaultGrantCatalogAccessUseCase(
            final AccountGateway accounts, final IdentityProviderGateway identityProvider) {
        this.accounts = Objects.requireNonNull(accounts, "'accounts' should not be null");
        this.identityProvider =
                Objects.requireNonNull(identityProvider, "'identityProvider' should not be null");
    }

    @Override
    public void execute(final String accountId) {
        this.identityProvider.grantCatalogAccess(accountOf(accountId).userId());
    }

    // Conta que não existe é erro, e precisa ser: o evento veio de uma assinatura que tem chave
    // estrangeira para a conta, então ela existia quando o fato aconteceu. Não existir agora
    // significa que alguém a apagou por baixo, e engolir isso deixaria acesso sem dono.
    private Account accountOf(final String accountId) {
        final var id = AccountId.from(accountId);
        return this.accounts.accountOfId(id).orElseThrow(() -> NotFoundException.with(Account.class, id));
    }
}
