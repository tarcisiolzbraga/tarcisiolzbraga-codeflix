package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models;

import com.tarcisiolzbraga.codeflix.subscription.application.account.AccountOutput;

// A conta como o corpo da resposta a mostra. Convertida do output por from(...): nenhum tipo da
// application aparece no JSON nem no schema OpenAPI.
public record AccountResponse(
        String id,
        String email,
        String firstname,
        String lastname,
        String documentNumber,
        String documentType,
        BillingAddressResponse billingAddress) {

    public static AccountResponse from(final AccountOutput output) {
        return new AccountResponse(
                output.id(),
                output.email(),
                output.firstname(),
                output.lastname(),
                output.documentNumber(),
                output.documentType(),
                BillingAddressResponse.from(output.billingAddress()));
    }

    // Nulo no JSON quando a conta não tem endereço de cobrança: a ausência é informação, e um
    // objeto de campos vazios a esconderia.
    public record BillingAddressResponse(String zipcode, String number, String complement, String country) {

        public static BillingAddressResponse from(final AccountOutput.AddressOutput output) {
            return output == null
                    ? null
                    : new BillingAddressResponse(
                            output.zipcode(), output.number(), output.complement(), output.country());
        }
    }
}
