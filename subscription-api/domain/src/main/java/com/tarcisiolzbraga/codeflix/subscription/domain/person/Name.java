package com.tarcisiolzbraga.codeflix.subscription.domain.person;

import com.tarcisiolzbraga.codeflix.subscription.domain.ValueObject;

public record Name(String firstname, String lastname) implements ValueObject {

    private static final int MAX_LENGTH = 255;

    public Name {
        this.assertArgumentNotEmpty(firstname, "'firstname' should not be empty");
        this.assertArgumentMaxLength(
                firstname, MAX_LENGTH, "'firstname' should not have more than %d characters".formatted(MAX_LENGTH));
        this.assertArgumentNotEmpty(lastname, "'lastname' should not be empty");
        this.assertArgumentMaxLength(
                lastname, MAX_LENGTH, "'lastname' should not have more than %d characters".formatted(MAX_LENGTH));
    }

    public String fullname() {
        return "%s %s".formatted(this.firstname, this.lastname);
    }
}
