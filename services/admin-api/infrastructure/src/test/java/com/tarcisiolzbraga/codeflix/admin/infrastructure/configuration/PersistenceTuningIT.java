package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class PersistenceTuningIT {

    private static final String TRUE = "true";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void givenHikariPool_whenReadSettings_thenLeaveTransactionsToHibernate() throws SQLException {
        final var pool = this.dataSource.unwrap(HikariDataSource.class);

        assertFalse(pool.isAutoCommit());
        assertEquals(2000, pool.getConnectionTimeout());
        assertEquals(600000, pool.getMaxLifetime());
        assertEquals(pool.getMaximumPoolSize(), pool.getMinimumIdle());
    }

    @Test
    void givenHikariPool_whenReadDriverProperties_thenCacheAndBatchStatements() throws SQLException {
        final var driverProperties = this.dataSource.unwrap(HikariDataSource.class).getDataSourceProperties();

        assertEquals(TRUE, driverProperties.getProperty("cachePrepStmts"));
        assertEquals(TRUE, driverProperties.getProperty("useServerPrepStmts"));
        assertEquals(TRUE, driverProperties.getProperty("rewriteBatchedStatements"));
    }

    @Test
    void givenHibernate_whenReadSettings_thenBatchAndDelayConnection() {
        final var properties = this.entityManagerFactory.getProperties();

        assertEquals(TRUE, properties.get("hibernate.connection.provider_disables_autocommit"));
        assertEquals("50", properties.get("hibernate.jdbc.batch_size"));
        assertEquals(TRUE, properties.get("hibernate.order_inserts"));
        assertEquals(TRUE, properties.get("hibernate.order_updates"));
    }
}
