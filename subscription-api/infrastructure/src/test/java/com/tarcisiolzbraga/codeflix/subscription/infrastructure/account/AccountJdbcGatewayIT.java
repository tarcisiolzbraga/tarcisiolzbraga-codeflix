package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.Account;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountCommand;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.UserId;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Document;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Name;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@IntegrationTest
class AccountJdbcGatewayIT {

    @Autowired
    private AccountJdbcGateway gateway;

    @Autowired
    private JdbcClient jdbcClient;

    // O meio de pagamento vai e volta pelo banco, e conta que nunca gravou um volta com nulo.
    @Test
    void givenAnAccountWithAPaymentMethod_whenSavedAndReadBack_thenTheReferenceSurvives() {
        final var account = AccountFixture.account();
        account.execute(new AccountCommand.ChangePaymentMethod(AccountFixture.paymentMethod()));

        gateway.save(account);

        final var actual = gateway.accountOfId(account.id()).orElseThrow();
        assertEquals(AccountFixture.paymentMethod(), actual.paymentMethod());
    }

    @Test
    void givenAnAccountWithoutAPaymentMethod_whenReadBack_thenTheFieldComesNull() {
        final var account = AccountFixture.account();

        gateway.save(account);

        assertNull(gateway.accountOfId(account.id()).orElseThrow().paymentMethod());
    }

    @Test
    void givenANewAccount_whenSavesIt_thenItCanBeReadBackWithTheSameValues() {
        final var account = AccountFixture.account();

        gateway.save(account);

        final var actual = gateway.accountOfId(account.id()).orElseThrow();
        assertEquals(AccountFixture.email(), actual.email());
        assertEquals(AccountFixture.name(), actual.name());
    }

    @Test
    void givenANewAccount_whenSavesIt_thenTheRowIsStoredAtVersionOne() {
        final var account = AccountFixture.account();

        gateway.save(account);

        assertEquals(1, gateway.accountOfId(account.id()).orElseThrow().version());
    }

    // A conta nasce sem endereço, e volta com nulo — não com um Address de campos vazios.
    @Test
    void givenAnAccountWithoutBillingAddress_whenReadsItBack_thenTheAddressIsAbsent() {
        final var account = AccountFixture.account();

        gateway.save(account);

        assertNull(gateway.accountOfId(account.id()).orElseThrow().billingAddress());
    }

    @Test
    void givenAStoredAccount_whenItsBillingAddressIsSet_thenItComesBackComplete() {
        final var stored = savedAccount();
        final var address = AccountFixture.billingAddress();
        stored.execute(new AccountCommand.ChangeBillingAddress(address));

        gateway.save(stored);

        assertEquals(address, gateway.accountOfId(stored.id()).orElseThrow().billingAddress());
    }

    @Test
    void givenACpfAccount_whenReadsItBack_thenTheDocumentIsRebuiltAsACpf() {
        final var account = AccountFixture.account();

        gateway.save(account);

        final var actual = gateway.accountOfId(account.id()).orElseThrow().document();
        assertInstanceOf(Document.Cpf.class, actual);
        assertEquals(AccountFixture.CPF, actual.value());
    }

    @Test
    void givenACnpjAccount_whenReadsItBack_thenTheDocumentIsRebuiltAsACnpj() {
        final var stored = savedAccount();
        stored.execute(new AccountCommand.ChangeDocument(Document.create("11222333000181", "cnpj")));

        gateway.save(stored);

        final var actual = gateway.accountOfId(stored.id()).orElseThrow().document();
        assertInstanceOf(Document.Cnpj.class, actual);
    }

    @Test
    void givenAStoredAccount_whenSavesItAgain_thenTheVersionMovesForward() {
        final var stored = savedAccount();
        stored.execute(new AccountCommand.ChangeProfile(new Name("Maria", "Silva")));

        gateway.save(stored);

        assertEquals(2, gateway.accountOfId(stored.id()).orElseThrow().version());
    }

    @Test
    void givenAnAccountAtAVersionThatIsNoLongerCurrent_whenSavesIt_thenRefusesWithConflict() {
        final var stored = savedAccount();
        stored.execute(new AccountCommand.ChangeProfile(new Name("Maria", "Silva")));
        gateway.save(stored);

        final var actual = assertThrows(ConflictException.class, () -> gateway.save(stored));

        assertEquals(
                "Account with id %s was changed by someone else; read it again".formatted(
                        stored.id().getValue()),
                actual.getMessage());
    }

    // O cadastro usa esta busca para saber se já existe conta para o e-mail.
    @Test
    void givenAStoredAccount_whenLooksItUpByEmail_thenFindsIt() {
        final var account = AccountFixture.account();
        gateway.save(account);

        final var actual = gateway.accountOfEmail(AccountFixture.email()).orElseThrow();

        assertEquals(account.id(), actual.id());
    }

    @Test
    void givenAnEmailWithNoAccount_whenLooksItUp_thenNothingComesBack() {
        gateway.save(AccountFixture.account());

        final var actual = gateway.accountOfEmail(new Email("ninguem@codeflix.com.br"));

        assertTrue(actual.isEmpty());
    }

    // É por aqui que a rota de autoatendimento acha a conta de quem está chamando, pelo `sub` do
    // token.
    @Test
    void givenAStoredAccount_whenLooksItUpByTheProviderId_thenFindsIt() {
        final var account = AccountFixture.account();
        gateway.save(account);

        final var actual = gateway.accountOfUserId(AccountFixture.userId()).orElseThrow();

        assertEquals(account.id(), actual.id());
    }

    @Test
    void givenAProviderIdWithNoAccount_whenLooksItUp_thenNothingComesBack() {
        gateway.save(AccountFixture.account());

        final var actual = gateway.accountOfUserId(new UserId("ninguem"));

        assertTrue(actual.isEmpty());
    }

    @Test
    void givenAMissingId_whenReadsIt_thenNothingComesBack() {
        final var anId = AccountId.unique();

        final var actual = gateway.accountOfId(anId);

        assertTrue(actual.isEmpty());
    }

    // O e-mail é por onde a pessoa entra: dois cadastros com o mesmo não podem coexistir, e quem
    // garante isso é a coluna, não só a checagem do caso de uso.
    @Test
    void givenAnEmailAlreadyTaken_whenSavesAnotherAccount_thenTheColumnRefuses() {
        gateway.save(AccountFixture.account());
        final var other = Account.newAccount(
                AccountId.unique(),
                new UserId("outro-id"),
                AccountFixture.email(),
                AccountFixture.name(),
                AccountFixture.document());

        assertThrows(DataIntegrityViolationException.class, () -> gateway.save(other));
    }

    @Test
    void givenAProviderIdAlreadyTaken_whenSavesAnotherAccount_thenTheColumnRefuses() {
        gateway.save(AccountFixture.account());
        final var other = Account.newAccount(
                AccountId.unique(),
                AccountFixture.userId(),
                new Email("outro@codeflix.com.br"),
                AccountFixture.name(),
                AccountFixture.document());

        assertThrows(DataIntegrityViolationException.class, () -> gateway.save(other));
    }

    // O endereço é tudo ou nada, e isto prova que o banco afirma a regra: o SQL abaixo não passa
    // pelo domínio, e mesmo assim é recusado.
    //
    // Afirma pelo nome da constraint, e não pelo tipo da exceção: o Spring traduz violação de CHECK
    // do MySQL para UncategorizedSQLException, não para DataIntegrityViolationException como faz com
    // UNIQUE — e o nome prova que foi esta regra que recusou, não outra qualquer.
    @Test
    void givenRawSqlWithHalfAnAddress_whenItIsWritten_thenTheCheckConstraintRefuses() {
        final var sql = """
                INSERT INTO account (id, version, idp_user_id, email, firstname, lastname,
                                     document_number, document_type, address_zipcode)
                VALUES (:id, 1, 'algum-id', 'meio@codeflix.com.br', 'Meio', 'Endereco',
                        '11144477735', 'cpf', '01310930')
                """;
        final var params = Map.<String, Object>of("id", AccountId.unique().getValue());

        final var actual =
                assertThrows(DataAccessException.class, () -> jdbcClient.sql(sql).params(params).update());

        assertTrue(
                actual.getMessage().contains("ck_account_billing_address"),
                "a mensagem deveria citar a constraint, e veio: " + actual.getMessage());
    }

    private Account savedAccount() {
        final var account = AccountFixture.account();
        gateway.save(account);
        return gateway.accountOfId(account.id()).orElseThrow();
    }
}
