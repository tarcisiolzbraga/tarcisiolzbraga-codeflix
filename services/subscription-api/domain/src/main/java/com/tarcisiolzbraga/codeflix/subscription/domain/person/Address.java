package com.tarcisiolzbraga.codeflix.subscription.domain.person;

import com.tarcisiolzbraga.codeflix.subscription.domain.ValueObject;

// O complemento é o único opcional: nem todo endereço tem um.
public record Address(String zipcode, String number, String complement, String country) implements ValueObject {

    private static final int MAX_LENGTH = 255;

    public Address {
        this.assertArgumentNotEmpty(zipcode, "'zipcode' should not be empty");
        this.assertArgumentMaxLength(zipcode, MAX_LENGTH, tooLong("zipcode"));
        this.assertArgumentNotEmpty(number, "'number' should not be empty");
        this.assertArgumentMaxLength(number, MAX_LENGTH, tooLong("number"));
        this.assertArgumentMaxLength(complement, MAX_LENGTH, tooLong("complement"));
        this.assertArgumentNotEmpty(country, "'country' should not be empty");
        this.assertArgumentMaxLength(country, MAX_LENGTH, tooLong("country"));
    }

    private static String tooLong(final String field) {
        return "'%s' should not have more than %d characters".formatted(field, MAX_LENGTH);
    }
}
