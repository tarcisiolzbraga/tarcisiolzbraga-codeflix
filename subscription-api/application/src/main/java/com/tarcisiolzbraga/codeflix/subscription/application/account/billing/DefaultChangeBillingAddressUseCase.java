package com.tarcisiolzbraga.codeflix.subscription.application.account.billing;

import com.tarcisiolzbraga.codeflix.subscription.application.account.AccountOutput;
import com.tarcisiolzbraga.codeflix.subscription.application.account.CallerAccount;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountCommand;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Address;
import java.util.Objects;

public class DefaultChangeBillingAddressUseCase extends ChangeBillingAddressUseCase {

    private final AccountGateway accountGateway;

    public DefaultChangeBillingAddressUseCase(final AccountGateway accountGateway) {
        this.accountGateway = Objects.requireNonNull(accountGateway, "'accountGateway' should not be null");
    }

    @Override
    public AccountOutput execute(final ChangeBillingAddressCommand command) {
        Objects.requireNonNull(command, "'command' should not be null");
        final var account = CallerAccount.of(this.accountGateway, command.userId());

        // O value object recusa endereço incompleto antes de qualquer gravação.
        final var address =
                new Address(command.zipcode(), command.number(), command.complement(), command.country());
        account.execute(new AccountCommand.ChangeBillingAddress(address));

        return AccountOutput.from(this.accountGateway.save(account));
    }
}
