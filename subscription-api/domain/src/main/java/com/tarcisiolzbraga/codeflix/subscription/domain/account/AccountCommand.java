package com.tarcisiolzbraga.codeflix.subscription.domain.account;

import com.tarcisiolzbraga.codeflix.subscription.domain.person.Address;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Document;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Name;

// Um comando por regra. Cada um carrega um value object já validado, então o agregado não revalida
// o que o tipo garante.
public sealed interface AccountCommand {

    record ChangeProfile(Name name) implements AccountCommand {}

    record ChangeEmail(Email email) implements AccountCommand {}

    record ChangeDocument(Document document) implements AccountCommand {}

    record ChangeBillingAddress(Address billingAddress) implements AccountCommand {}
}
