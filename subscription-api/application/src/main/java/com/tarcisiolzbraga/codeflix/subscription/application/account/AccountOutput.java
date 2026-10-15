package com.tarcisiolzbraga.codeflix.subscription.application.account;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.Account;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Address;

// A conta como a aplicação a entrega. O id sai como texto: a borda é o JSON, e é aqui que a
// conversão acontece.
//
// Sem a senha e sem o id do provedor de identidade: a primeira nunca sai daqui, e o segundo é
// detalhe de como a autenticação funciona — quem chama já sabe quem é.
public record AccountOutput(
        String id,
        String email,
        String firstname,
        String lastname,
        String documentNumber,
        String documentType,
        AddressOutput billingAddress,
        boolean hasPaymentMethod) {

    public static AccountOutput from(final Account anAccount) {
        return new AccountOutput(
                anAccount.id().getValue(),
                anAccount.email().value(),
                anAccount.name().firstname(),
                anAccount.name().lastname(),
                anAccount.document().value(),
                anAccount.document().type(),
                AddressOutput.from(anAccount.billingAddress()),
                anAccount.paymentMethod() != null);
    }

    // Um booleano, e **não a referência do meio de pagamento**: o front só precisa saber se tem de
    // pedir um cartão. A referência não serve a quem chama, e mandá-la seria entregar um
    // identificador do provedor sem nenhuma razão.
    //
    // Booleano escolhendo entre dois comportamentos é proibido no monorepo; isto é booleano
    // descrevendo um estado, que é outra coisa.

    // Nulo quando a conta não tem endereço de cobrança, e não um endereço de campos vazios: a
    // ausência é informação.
    public record AddressOutput(String zipcode, String number, String complement, String country) {

        public static AddressOutput from(final Address anAddress) {
            return anAddress == null
                    ? null
                    : new AddressOutput(
                            anAddress.zipcode(), anAddress.number(), anAddress.complement(), anAddress.country());
        }
    }
}
