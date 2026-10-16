package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.signup;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.UserId;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUp;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUpGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUpId;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpToken;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Document;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Name;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc.DatabaseClient;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc.JdbcUtils;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc.RowMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PendingSignUpJdbcGateway implements PendingSignUpGateway {

    private static final String COLUMNS = """
            id, version, idp_user_id, email, firstname, lastname, document_number, document_type,
            token_hash, expires_at, confirmed_at""";

    private final DatabaseClient database;

    public PendingSignUpJdbcGateway(final DatabaseClient database) {
        this.database = Objects.requireNonNull(database, "'database' should not be null");
    }

    // Procura pelo resumo, que é o que está gravado: o token em claro nunca entra numa consulta.
    @Override
    public Optional<PendingSignUp> pendingSignUpOfToken(final SignUpToken aToken) {
        final var sql = "SELECT %s FROM pending_sign_up WHERE token_hash = :tokenHash".formatted(COLUMNS);
        return this.database.queryOne(sql, Map.of("tokenHash", aToken.hashed()), pendingSignUpMapper());
    }

    @Override
    public PendingSignUp save(final PendingSignUp aPendingSignUp) {
        if (aPendingSignUp.version() == 0) {
            insert(aPendingSignUp);
        } else {
            update(aPendingSignUp);
        }
        return aPendingSignUp;
    }

    @Override
    public int deleteUnconfirmedOf(final Email anEmail) {
        final var sql = "DELETE FROM pending_sign_up WHERE email = :email AND confirmed_at IS NULL";
        return this.database.update(sql, Map.of("email", anEmail.value()));
    }

    private void insert(final PendingSignUp aPendingSignUp) {
        final var sql = """
                INSERT INTO pending_sign_up (%s)
                VALUES (:id, 1, :userId, :email, :firstname, :lastname, :documentNumber, :documentType,
                        :tokenHash, :expiresAt, :confirmedAt)
                """
                .formatted(COLUMNS);
        this.database.update(sql, paramsOf(aPendingSignUp));
    }

    // Só o que a confirmação muda. O resto da linha é do cadastro e não se reescreve.
    private void update(final PendingSignUp aPendingSignUp) {
        final var sql = """
                UPDATE pending_sign_up SET
                    version = :version + 1,
                    confirmed_at = :confirmedAt
                WHERE id = :id AND version = :version
                """;
        if (this.database.update(sql, paramsOf(aPendingSignUp)) == 0) {
            throw ConflictException.staleVersion(PendingSignUp.class, aPendingSignUp.id());
        }
    }

    private Map<String, Object> paramsOf(final PendingSignUp aPendingSignUp) {
        final var params = new HashMap<String, Object>();
        params.put("id", aPendingSignUp.id().getValue());
        params.put("version", aPendingSignUp.version());
        params.put("userId", aPendingSignUp.userId().value());
        params.put("email", aPendingSignUp.email().value());
        params.put("firstname", aPendingSignUp.name().firstname());
        params.put("lastname", aPendingSignUp.name().lastname());
        params.put("documentNumber", aPendingSignUp.document().value());
        params.put("documentType", aPendingSignUp.document().type());
        params.put("tokenHash", aPendingSignUp.tokenHash());
        params.put("expiresAt", aPendingSignUp.expiresAt());
        params.put("confirmedAt", aPendingSignUp.confirmedAt());
        return params;
    }

    private static RowMap<PendingSignUp> pendingSignUpMapper() {
        return rs -> PendingSignUp.with(
                PendingSignUpId.from(rs.getString("id")),
                rs.getInt("version"),
                new UserId(rs.getString("idp_user_id")),
                new Email(rs.getString("email")),
                new Name(rs.getString("firstname"), rs.getString("lastname")),
                Document.create(rs.getString("document_number"), rs.getString("document_type")),
                rs.getString("token_hash"),
                JdbcUtils.getInstant(rs, "expires_at"),
                JdbcUtils.getInstant(rs, "confirmed_at"));
    }
}
