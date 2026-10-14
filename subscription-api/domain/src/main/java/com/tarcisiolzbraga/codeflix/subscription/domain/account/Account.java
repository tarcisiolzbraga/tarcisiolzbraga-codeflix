package com.tarcisiolzbraga.codeflix.subscription.domain.account;

import com.tarcisiolzbraga.codeflix.subscription.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.UserId;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Address;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Document;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Name;

// Quem assina: a pessoa, o vínculo dela com o provedor de identidade e o endereço de cobrança.
//
// Não tem data alguma, diferente do Plan e da Subscription: nada na conta se decide por tempo. É o
// exemplo de por que `createdAt` e `updatedAt` não subiram para o AggregateRoot.
//
// Não tem estado de validação de e-mail: a conta só nasce depois de o e-mail ser confirmado, então
// uma conta existir já significa que ele foi. O cadastro pendente é outra coisa, e vive na própria
// tabela dele.
public class Account extends AggregateRoot<AccountId> {

    private UserId userId;
    private Email email;
    private Name name;
    private Document document;
    private Address billingAddress;

    private Account(
            final AccountId anId,
            final int version,
            final UserId anUserId,
            final Email anEmail,
            final Name aName,
            final Document aDocument,
            final Address billingAddress) {
        super(anId, version);
        this.setUserId(anUserId);
        this.setEmail(anEmail);
        this.setName(aName);
        this.setDocument(aDocument);
        this.setBillingAddress(billingAddress);
    }

    // A conta nasce sem endereço de cobrança: ele é pedido na hora de cobrar, não no cadastro.
    public static Account newAccount(
            final AccountId anId,
            final UserId anUserId,
            final Email anEmail,
            final Name aName,
            final Document aDocument) {
        final var anAccount = new Account(anId, 0, anUserId, anEmail, aName, aDocument, null);
        anAccount.registerEvent(AccountCreated.from(anAccount));
        return anAccount;
    }

    public static Account with(
            final AccountId anId,
            final int version,
            final UserId anUserId,
            final Email anEmail,
            final Name aName,
            final Document aDocument,
            final Address billingAddress) {
        return new Account(anId, version, anUserId, anEmail, aName, aDocument, billingAddress);
    }

    // Não toca na versão: ela é do lock otimista, e quem a incrementa é o UPDATE do repositório.
    public void execute(final AccountCommand... commands) {
        if (commands == null || commands.length == 0) {
            return;
        }

        for (final var command : commands) {
            switch (command) {
                case AccountCommand.ChangeProfile c -> this.setName(c.name());
                case AccountCommand.ChangeEmail c -> this.setEmail(c.email());
                case AccountCommand.ChangeDocument c -> this.setDocument(c.document());
                case AccountCommand.ChangeBillingAddress c -> this.setBillingAddress(c.billingAddress());
            }
        }
    }

    public UserId userId() {
        return this.userId;
    }

    public Email email() {
        return this.email;
    }

    public Name name() {
        return this.name;
    }

    public Document document() {
        return this.document;
    }

    public Address billingAddress() {
        return this.billingAddress;
    }

    private void setUserId(final UserId userId) {
        this.userId = this.assertArgumentNotNull(userId, "'userId' should not be null");
    }

    private void setEmail(final Email email) {
        this.email = this.assertArgumentNotNull(email, "'email' should not be null");
    }

    private void setName(final Name name) {
        this.name = this.assertArgumentNotNull(name, "'name' should not be null");
    }

    private void setDocument(final Document document) {
        this.document = this.assertArgumentNotNull(document, "'document' should not be null");
    }

    // O único que aceita nulo: a conta nasce sem endereço de cobrança.
    private void setBillingAddress(final Address billingAddress) {
        this.billingAddress = billingAddress;
    }
}
