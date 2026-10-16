package com.tarcisiolzbraga.codeflix.subscription.application.account.billing;

import com.tarcisiolzbraga.codeflix.subscription.application.UseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.AccountOutput;

public abstract class ChangeBillingAddressUseCase
        extends UseCase<ChangeBillingAddressCommand, AccountOutput> {}
