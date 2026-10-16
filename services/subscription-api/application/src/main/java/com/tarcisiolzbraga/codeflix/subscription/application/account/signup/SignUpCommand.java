package com.tarcisiolzbraga.codeflix.subscription.application.account.signup;

public record SignUpCommand(
        String firstname,
        String lastname,
        String email,
        String password,
        String documentNumber,
        String documentType) {

    public static SignUpCommand with(
            final String firstname,
            final String lastname,
            final String email,
            final String password,
            final String documentNumber,
            final String documentType) {
        return new SignUpCommand(firstname, lastname, email, password, documentNumber, documentType);
    }
}
