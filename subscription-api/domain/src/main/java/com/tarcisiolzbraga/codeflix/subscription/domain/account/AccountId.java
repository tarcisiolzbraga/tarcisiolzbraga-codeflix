package com.tarcisiolzbraga.codeflix.subscription.domain.account;

import com.tarcisiolzbraga.codeflix.subscription.domain.Identifier;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import java.util.UUID;

// UUID, como os outros ids deste monorepo. A referência usa CHAR(32) sem hífen.
public record AccountId(UUID value) implements Identifier {

    public AccountId {
        if (value == null) {
            throw DomainException.with("'accountId' should not be null");
        }
    }

    public static AccountId unique() {
        return new AccountId(UUID.randomUUID());
    }

    public static AccountId from(final String value) {
        return new AccountId(Identifier.uuidOf(value, AccountId.class));
    }
}
