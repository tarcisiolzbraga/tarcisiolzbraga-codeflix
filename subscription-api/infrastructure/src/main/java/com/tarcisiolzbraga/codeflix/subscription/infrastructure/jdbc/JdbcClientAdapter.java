package com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

public class JdbcClientAdapter implements DatabaseClient {

    private final JdbcClient target;

    public JdbcClientAdapter(final JdbcClient target) {
        this.target = Objects.requireNonNull(target, "'target' should not be null");
    }

    @Override
    public <T> Optional<T> queryOne(final String sql, final Map<String, Object> params, final RowMap<T> mapper) {
        return this.target.sql(sql).params(params).query((rs, rowNum) -> mapper.mapRow(rs)).optional();
    }

    @Override
    public <T> List<T> query(final String sql, final Map<String, Object> params, final RowMap<T> mapper) {
        return this.target.sql(sql).params(params).query((rs, rowNum) -> mapper.mapRow(rs)).list();
    }

    @Override
    public int update(final String sql, final Map<String, Object> params) {
        return this.target.sql(sql).params(params).update();
    }
}
