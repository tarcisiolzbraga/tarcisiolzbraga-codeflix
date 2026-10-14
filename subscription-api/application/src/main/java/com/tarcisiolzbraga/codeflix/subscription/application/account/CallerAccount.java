package com.tarcisiolzbraga.codeflix.subscription.application.account;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.Account;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.UserId;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;

// Encontrar a conta de quem está chamando, pelo id dele no provedor de identidade. Os dois casos de
// uso de autoatendimento começam por isto, e a mensagem de "não achei" tem de ser a mesma nos dois.
//
// Não diz qual id não foi encontrado: o id vem do token de quem chamou, e repeti-lo na resposta não
// informa nada a quem já o tem.
public final class CallerAccount {

    private CallerAccount() {}

    public static Account of(final AccountGateway accounts, final String userId) {
        final var anUserId = new UserId(userId);
        return accounts.accountOfUserId(anUserId)
                .orElseThrow(() -> NotFoundException.callerAccount());
    }
}
