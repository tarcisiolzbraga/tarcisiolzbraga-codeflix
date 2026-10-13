package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.money.Money;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc.DatabaseClient;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc.JdbcUtils;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc.RowMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

// Sem entidade espelhando a tabela: o mapeador lê o ResultSet direto para dentro do agregado, pelo
// with(...) dele. É a consequência de não haver ORM, e a divergência registrada da regra de
// from(...)/toAggregate() dos outros serviços.
@Repository
public class PlanJdbcGateway implements PlanGateway {

    private static final String COLUMNS =
            "id, version, name, description, active, currency, amount, created_at, updated_at";

    private final DatabaseClient database;

    public PlanJdbcGateway(final DatabaseClient database) {
        this.database = Objects.requireNonNull(database, "'database' should not be null");
    }

    @Override
    public Optional<Plan> planOfId(final PlanId anId) {
        final var sql = "SELECT %s FROM plan WHERE id = :id".formatted(COLUMNS);
        return this.database.queryOne(sql, Map.of("id", anId.getValue()), planMapper());
    }

    // Versão zero é plano que nunca foi gravado: a linha nasce na versão 1, então nenhuma linha
    // guardada tem zero e a escolha entre inserir e atualizar não é ambígua.
    //
    // Devolve o agregado recebido, e não um remontado: remontar perderia os eventos de domínio que
    // ele ainda carrega. O preço é que a versão em memória continua sendo a usada no lock, e não a
    // que ficou gravada — para gravar de novo é preciso ler o plano outra vez.
    @Override
    public Plan save(final Plan aPlan) {
        if (aPlan.version() == 0) {
            insert(aPlan);
        } else {
            update(aPlan);
        }
        return aPlan;
    }

    private void insert(final Plan aPlan) {
        final var sql = """
                INSERT INTO plan (%s)
                VALUES (:id, 1, :name, :description, :active, :currency, :amount, :createdAt, :updatedAt)
                """
                .formatted(COLUMNS);
        this.database.update(sql, paramsOf(aPlan));
    }

    // O lock otimista: a versão entra no WHERE e sobe no SET. Zero linhas afetadas não quer dizer
    // que o id não existe — quer dizer que a linha não está mais na versão que foi lida.
    private void update(final Plan aPlan) {
        final var sql = """
                UPDATE plan SET
                    version = :version + 1,
                    name = :name,
                    description = :description,
                    active = :active,
                    currency = :currency,
                    amount = :amount,
                    updated_at = :updatedAt
                WHERE id = :id AND version = :version
                """;
        if (this.database.update(sql, paramsOf(aPlan)) == 0) {
            throw ConflictException.staleVersion(Plan.class, aPlan.id());
        }
    }

    // O created_at não entra no update: ele é do momento da criação e não se reescreve.
    private Map<String, Object> paramsOf(final Plan aPlan) {
        final var params = new HashMap<String, Object>();
        params.put("id", aPlan.id().getValue());
        params.put("version", aPlan.version());
        params.put("name", aPlan.name());
        params.put("description", aPlan.description());
        params.put("active", aPlan.active());
        params.put("currency", aPlan.price().currencyCode());
        params.put("amount", aPlan.price().amount());
        params.put("createdAt", aPlan.createdAt());
        params.put("updatedAt", aPlan.updatedAt());
        return params;
    }

    private static RowMap<Plan> planMapper() {
        return rs -> Plan.with(
                PlanId.from(rs.getString("id")),
                rs.getInt("version"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("active"),
                Money.of(rs.getString("currency"), rs.getBigDecimal("amount")),
                JdbcUtils.getInstant(rs, "created_at"),
                JdbcUtils.getInstant(rs, "updated_at"));
    }
}
