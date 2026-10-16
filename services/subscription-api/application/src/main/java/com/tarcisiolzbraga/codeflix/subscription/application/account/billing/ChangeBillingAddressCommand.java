package com.tarcisiolzbraga.codeflix.subscription.application.account.billing;

// O userId vem do token, não do corpo: quem chama só muda o próprio endereço.
public record ChangeBillingAddressCommand(
        String userId, String zipcode, String number, String complement, String country) {

    public static ChangeBillingAddressCommand with(
            final String userId,
            final String zipcode,
            final String number,
            final String complement,
            final String country) {
        return new ChangeBillingAddressCommand(userId, zipcode, number, complement, country);
    }
}
