package com.tarcisiolzbraga.codeflix.subscription.domain.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.UserId;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Address;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Document;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Name;
import org.junit.jupiter.api.Test;

class AccountTest {

    @Test
    void givenValidData_whenCreatesANewAccount_thenHoldsTheGivenValues() {
        final var id = AccountId.unique();

        final var actual = AccountFixture.accountWith(id);

        assertEquals(id, actual.id());
        assertEquals(AccountFixture.email(), actual.email());
    }

    @Test
    void givenValidData_whenCreatesANewAccount_thenItStartsAtVersionZeroBecauseItWasNeverStored() {
        final var actual = AccountFixture.account();

        final var version = actual.version();

        assertEquals(0, version);
    }

    // A conta nasce sem endereço de cobrança: ele é pedido na hora de cobrar, não no cadastro.
    @Test
    void givenValidData_whenCreatesANewAccount_thenThereIsNoBillingAddressYet() {
        final var actual = AccountFixture.account();

        final var billingAddress = actual.billingAddress();

        assertNull(billingAddress);
    }

    @Test
    void givenANewAccount_whenAsksForItsEvents_thenItRegisteredThatItWasCreated() {
        final var actual = AccountFixture.account();

        final var events = actual.domainEvents();

        assertEquals(1, events.size());
        assertInstanceOf(AccountCreated.class, events.getFirst());
    }

    @Test
    void givenANewAccount_whenAsksTheEvent_thenItCarriesTheAccountId() {
        final var account = AccountFixture.account();

        final var actual = (AccountCreated) account.domainEvents().getFirst();

        assertEquals(account.id().getValue(), actual.aggregateId());
        assertEquals("Account", actual.aggregateType());
    }

    // Reconstruir do banco não é fato novo: nada de evento.
    @Test
    void givenAnAccountComingFromStorage_whenRebuilt_thenThereIsNoEvent() {
        final var actual = AccountFixture.storedAccount();

        final var events = actual.domainEvents();

        assertTrue(events.isEmpty());
    }

    @Test
    void givenAnAccountComingFromStorage_whenRebuilt_thenTheVersionIsPreserved() {
        final var actual = AccountFixture.storedAccount(4);

        final var version = actual.version();

        assertEquals(4, version);
    }

    @Test
    void givenNullUserId_whenCreatesANewAccount_thenThrowsDomainException() {
        final UserId userId = null;

        final var actual = assertThrows(DomainException.class, () -> AccountFixture.accountWithUserId(userId));

        assertEquals("'userId' should not be null", actual.getMessage());
    }

    @Test
    void givenNullEmail_whenCreatesANewAccount_thenThrowsDomainException() {
        final Email email = null;

        final var actual = assertThrows(DomainException.class, () -> AccountFixture.accountWithEmail(email));

        assertEquals("'email' should not be null", actual.getMessage());
    }

    @Test
    void givenNullName_whenCreatesANewAccount_thenThrowsDomainException() {
        final Name name = null;

        final var actual = assertThrows(DomainException.class, () -> AccountFixture.accountWithName(name));

        assertEquals("'name' should not be null", actual.getMessage());
    }

    @Test
    void givenNullDocument_whenCreatesANewAccount_thenThrowsDomainException() {
        final Document document = null;

        final var actual = assertThrows(DomainException.class, () -> AccountFixture.accountWithDocument(document));

        assertEquals("'document' should not be null", actual.getMessage());
    }

    @Test
    void givenAStoredAccount_whenChangesTheProfile_thenTheNameIsTheNewOne() {
        final var account = AccountFixture.storedAccount();
        final var newName = new Name("Maria", "Silva");

        account.execute(new AccountCommand.ChangeProfile(newName));

        assertEquals(newName, account.name());
    }

    @Test
    void givenAStoredAccount_whenChangesTheEmail_thenItIsTheNewOne() {
        final var account = AccountFixture.storedAccount();
        final var newEmail = new Email("maria@codeflix.com.br");

        account.execute(new AccountCommand.ChangeEmail(newEmail));

        assertEquals(newEmail, account.email());
    }

    @Test
    void givenAStoredAccount_whenChangesTheDocument_thenItIsTheNewOne() {
        final var account = AccountFixture.storedAccount();
        final var newDocument = Document.create("11222333000181", "cnpj");

        account.execute(new AccountCommand.ChangeDocument(newDocument));

        assertEquals(newDocument, account.document());
    }

    @Test
    void givenAnAccountWithoutBillingAddress_whenItIsSet_thenItIsThere() {
        final var account = AccountFixture.storedAccount();
        final var address = new Address("01310930", "1578", "apto 42", "BR");

        account.execute(new AccountCommand.ChangeBillingAddress(address));

        assertEquals(address, account.billingAddress());
    }

    @Test
    void givenAStoredAccount_whenChangesTheProfile_thenTheVersionIsUntouched() {
        final var account = AccountFixture.storedAccount(4);

        account.execute(new AccountCommand.ChangeProfile(new Name("Maria", "Silva")));

        assertEquals(4, account.version());
    }

    @Test
    void givenAStoredAccount_whenChangesSomething_thenNoEventIsRegisteredBecauseNoOneListensYet() {
        final var account = AccountFixture.storedAccount();

        account.execute(new AccountCommand.ChangeProfile(new Name("Maria", "Silva")));

        assertTrue(account.domainEvents().isEmpty());
    }

    @Test
    void givenAStoredAccount_whenExecutesNull_thenNothingHappens() {
        final var account = AccountFixture.storedAccount();
        final var before = account.name();

        account.execute((AccountCommand[]) null);

        assertEquals(before, account.name());
    }
}
