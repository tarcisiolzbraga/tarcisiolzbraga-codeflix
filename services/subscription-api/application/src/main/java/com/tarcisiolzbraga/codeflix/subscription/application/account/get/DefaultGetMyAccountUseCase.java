package com.tarcisiolzbraga.codeflix.subscription.application.account.get;

import com.tarcisiolzbraga.codeflix.subscription.application.account.AccountOutput;
import com.tarcisiolzbraga.codeflix.subscription.application.account.CallerAccount;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import java.util.Objects;

public class DefaultGetMyAccountUseCase extends GetMyAccountUseCase {

    private final AccountGateway accountGateway;

    public DefaultGetMyAccountUseCase(final AccountGateway accountGateway) {
        this.accountGateway = Objects.requireNonNull(accountGateway, "'accountGateway' should not be null");
    }

    @Override
    public AccountOutput execute(final String userId) {
        return AccountOutput.from(CallerAccount.of(this.accountGateway, userId));
    }
}
