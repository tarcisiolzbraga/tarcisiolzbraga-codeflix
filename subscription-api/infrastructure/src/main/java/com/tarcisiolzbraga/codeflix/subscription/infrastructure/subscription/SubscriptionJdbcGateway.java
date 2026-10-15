package com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.DomainEventPublisher;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.Subscription;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionId;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionStatus;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc.DatabaseClient;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc.JdbcUtils;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc.RowMap;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SubscriptionJdbcGateway implements SubscriptionGateway {

    private static final String COLUMNS = """
            id, version, account_id, plan_id, status, due_date, last_renew_date, last_transaction_id,
            created_at, updated_at""";

    private final DatabaseClient database;
    private final DomainEventPublisher events;

    public SubscriptionJdbcGateway(final DatabaseClient database, final DomainEventPublisher events) {
        this.database = Objects.requireNonNull(database, "'database' should not be null");
        this.events = Objects.requireNonNull(events, "'events' should not be null");
    }

    // A mais recente pela criação, e não pelo vencimento: reassinar depois de cancelar cria uma
    // linha nova, e é essa que vale.
    @Override
    public Optional<Subscription> latestSubscriptionOfAccount(final AccountId anAccountId) {
        final var sql = """
                SELECT %s FROM subscription
                WHERE account_id = :accountId
                ORDER BY created_at DESC
                LIMIT 1
                """
                .formatted(COLUMNS);
        return this.database.queryOne(
                sql, Map.of("accountId", anAccountId.getValue()), subscriptionMapper());
    }

    // @Transactional porque são duas tabelas: a assinatura e a fila de saída. É isso que faz
    // "gravou e avisou" ser atômico — sem a transação, o pool entrega a conexão com auto-commit
    // ligado e a assinatura ficaria gravada com o evento perdido, ou o contrário.
    @Override
    @Transactional
    public Subscription save(final Subscription aSubscription) {
        if (aSubscription.version() == 0) {
            insert(aSubscription);
        } else {
            update(aSubscription);
        }
        aSubscription.publishDomainEvents(this.events);
        return aSubscription;
    }

    private void insert(final Subscription aSubscription) {
        final var sql = """
                INSERT INTO subscription (%s)
                VALUES (:id, 1, :accountId, :planId, :status, :dueDate, :lastRenewDate,
                        :lastTransactionId, :createdAt, :updatedAt)
                """
                .formatted(COLUMNS);
        this.database.update(sql, paramsOf(aSubscription));
    }

    // O created_at não entra: é do momento da criação e não se reescreve.
    private void update(final Subscription aSubscription) {
        final var sql = """
                UPDATE subscription SET
                    version = :version + 1,
                    plan_id = :planId,
                    status = :status,
                    due_date = :dueDate,
                    last_renew_date = :lastRenewDate,
                    last_transaction_id = :lastTransactionId,
                    updated_at = :updatedAt
                WHERE id = :id AND version = :version
                """;
        if (this.database.update(sql, paramsOf(aSubscription)) == 0) {
            throw ConflictException.staleVersion(Subscription.class, aSubscription.id());
        }
    }

    private Map<String, Object> paramsOf(final Subscription aSubscription) {
        final var params = new HashMap<String, Object>();
        params.put("id", aSubscription.id().getValue());
        params.put("version", aSubscription.version());
        params.put("accountId", aSubscription.accountId().getValue());
        params.put("planId", aSubscription.planId().getValue());
        params.put("status", aSubscription.status().value());
        params.put("dueDate", aSubscription.dueDate());
        params.put("lastRenewDate", aSubscription.lastRenewDate());
        params.put("lastTransactionId", aSubscription.lastTransactionId());
        params.put("createdAt", aSubscription.createdAt());
        params.put("updatedAt", aSubscription.updatedAt());
        return params;
    }

    private static RowMap<Subscription> subscriptionMapper() {
        return rs -> Subscription.with(
                SubscriptionId.from(rs.getString("id")),
                rs.getInt("version"),
                AccountId.from(rs.getString("account_id")),
                PlanId.from(rs.getString("plan_id")),
                SubscriptionStatus.of(rs.getString("status")),
                dueDateOf(rs),
                JdbcUtils.getInstant(rs, "last_renew_date"),
                rs.getString("last_transaction_id"),
                JdbcUtils.getInstant(rs, "created_at"),
                JdbcUtils.getInstant(rs, "updated_at"));
    }

    private static LocalDate dueDateOf(final ResultSet rs) throws SQLException {
        final var date = rs.getDate("due_date");
        return date == null ? null : date.toLocalDate();
    }
}
