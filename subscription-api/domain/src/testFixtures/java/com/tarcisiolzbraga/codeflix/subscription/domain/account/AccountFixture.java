package com.tarcisiolzbraga.codeflix.subscription.domain.account;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.UserId;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Address;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Document;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Name;

// Dados de teste da conta, iguais em todo teste que não se importa com os valores. Sem sorteio: o
// teste que falha falha sempre, e o bloco given continua dizendo o que importa para aquele caso.
public final class AccountFixture {

    public static final String USER_ID = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";
    public static final String EMAIL = "assinante@codeflix.com.br";
    public static final String FIRSTNAME = "Tarcisio";
    public static final String LASTNAME = "Braga";
    public static final String CPF = "11144477735";

    private AccountFixture() {}

    public static UserId userId() {
        return new UserId(USER_ID);
    }

    public static Email email() {
        return new Email(EMAIL);
    }

    public static Name name() {
        return new Name(FIRSTNAME, LASTNAME);
    }

    public static Document document() {
        return Document.create(CPF, Document.Cpf.TYPE);
    }

    public static Address billingAddress() {
        return new Address("01310930", "1578", "apto 42", "BR");
    }

    public static Account account() {
        return accountWith(AccountId.unique());
    }

    public static Account accountWith(final AccountId anId) {
        return Account.newAccount(anId, userId(), email(), name(), document());
    }

    public static Account accountWithUserId(final UserId anUserId) {
        return Account.newAccount(AccountId.unique(), anUserId, email(), name(), document());
    }

    public static Account accountWithEmail(final Email anEmail) {
        return Account.newAccount(AccountId.unique(), userId(), anEmail, name(), document());
    }

    public static Account accountWithName(final Name aName) {
        return Account.newAccount(AccountId.unique(), userId(), email(), aName, document());
    }

    public static Account accountWithDocument(final Document aDocument) {
        return Account.newAccount(AccountId.unique(), userId(), email(), name(), aDocument);
    }

    // Uma conta como ela volta do banco: com versão e sem evento nenhum pendente.
    public static Account storedAccount() {
        return storedAccount(1);
    }

    public static Account storedAccount(final int version) {
        return Account.with(AccountId.unique(), version, userId(), email(), name(), document(), null);
    }
}
