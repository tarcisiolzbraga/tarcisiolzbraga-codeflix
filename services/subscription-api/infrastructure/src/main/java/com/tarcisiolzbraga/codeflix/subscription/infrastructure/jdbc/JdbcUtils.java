package com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public final class JdbcUtils {

    private JdbcUtils() {}

    // Coluna nula devolve nulo, e não uma data qualquer: é o que distingue "não aconteceu" de
    // "aconteceu na época zero".
    public static Instant getInstant(final ResultSet rs, final String column) throws SQLException {
        final var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
