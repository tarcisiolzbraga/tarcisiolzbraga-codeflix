package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.Account;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.UserId;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Address;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Document;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Name;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc.DatabaseClient;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc.RowMap;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

// Sem entidade espelhando a tabela: o mapeador lê o ResultSet direto para dentro do agregado.
@Repository
public class AccountJdbcGateway implements AccountGateway {

    private static final String COLUMNS = """
            id, version, idp_user_id, email, firstname, lastname, document_number, document_type,
            address_zipcode, address_number, address_complement, address_country""";

    private final DatabaseClient database;

    public AccountJdbcGateway(final DatabaseClient database) {
        this.database = Objects.requireNonNull(database, "'database' should not be null");
    }

    @Override
    public Optional<Account> accountOfId(final AccountId anId) {
        final var sql = "SELECT %s FROM account WHERE id = :id".formatted(COLUMNS);
        return this.database.queryOne(sql, Map.of("id", anId.getValue()), accountMapper());
    }

    @Override
    public Account save(final Account anAccount) {
        if (anAccount.version() == 0) {
            insert(anAccount);
        } else {
            update(anAccount);
        }
        return anAccount;
    }

    private void insert(final Account anAccount) {
        final var sql = """
                INSERT INTO account (%s)
                VALUES (:id, 1, :userId, :email, :firstname, :lastname, :documentNumber, :documentType,
                        :zipcode, :number, :complement, :country)
                """
                .formatted(COLUMNS);
        this.database.update(sql, paramsOf(anAccount));
    }

    private void update(final Account anAccount) {
        final var sql = """
                UPDATE account SET
                    version = :version + 1,
                    idp_user_id = :userId,
                    email = :email,
                    firstname = :firstname,
                    lastname = :lastname,
                    document_number = :documentNumber,
                    document_type = :documentType,
                    address_zipcode = :zipcode,
                    address_number = :number,
                    address_complement = :complement,
                    address_country = :country
                WHERE id = :id AND version = :version
                """;
        if (this.database.update(sql, paramsOf(anAccount)) == 0) {
            throw ConflictException.staleVersion(Account.class, anAccount.id());
        }
    }

    private Map<String, Object> paramsOf(final Account anAccount) {
        final var params = new HashMap<String, Object>();
        params.put("id", anAccount.id().getValue());
        params.put("version", anAccount.version());
        params.put("userId", anAccount.userId().value());
        params.put("email", anAccount.email().value());
        params.put("firstname", anAccount.name().firstname());
        params.put("lastname", anAccount.name().lastname());
        params.put("documentNumber", anAccount.document().value());
        params.put("documentType", anAccount.document().type());
        params.putAll(addressParamsOf(anAccount.billingAddress()));
        return params;
    }

    // As quatro colunas vão juntas, nulas ou preenchidas: é a mesma regra que o CHECK da tabela
    // afirma, e deixar uma de fora do mapa faria o insert falhar por parâmetro ausente.
    private Map<String, Object> addressParamsOf(final Address address) {
        final var params = new HashMap<String, Object>();
        params.put("zipcode", address == null ? null : address.zipcode());
        params.put("number", address == null ? null : address.number());
        params.put("complement", address == null ? null : address.complement());
        params.put("country", address == null ? null : address.country());
        return params;
    }

    private static RowMap<Account> accountMapper() {
        return rs -> Account.with(
                AccountId.from(rs.getString("id")),
                rs.getInt("version"),
                new UserId(rs.getString("idp_user_id")),
                new Email(rs.getString("email")),
                new Name(rs.getString("firstname"), rs.getString("lastname")),
                Document.create(rs.getString("document_number"), rs.getString("document_type")),
                addressOf(rs));
    }

    // Conta sem endereço de cobrança volta com nulo, não com um Address de campos vazios.
    private static Address addressOf(final ResultSet rs) throws SQLException {
        final var zipcode = rs.getString("address_zipcode");
        if (zipcode == null) {
            return null;
        }
        return new Address(
                zipcode,
                rs.getString("address_number"),
                rs.getString("address_complement"),
                rs.getString("address_country"));
    }
}
