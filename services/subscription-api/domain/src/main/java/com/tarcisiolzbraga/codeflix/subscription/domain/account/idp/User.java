package com.tarcisiolzbraga.codeflix.subscription.domain.account.idp;

import com.tarcisiolzbraga.codeflix.subscription.domain.ValueObject;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Name;

// A pessoa como o provedor de identidade a descreve. Sem senha: ela é dado de criação, e o provedor
// nunca a devolve.
public record User(UserId userId, Name name, Email email, boolean enabled, boolean emailVerified)
        implements ValueObject {

    public User {
        this.assertArgumentNotNull(userId, "'userId' should not be null");
        this.assertArgumentNotNull(name, "'name' should not be null");
        this.assertArgumentNotNull(email, "'email' should not be null");
    }
}
