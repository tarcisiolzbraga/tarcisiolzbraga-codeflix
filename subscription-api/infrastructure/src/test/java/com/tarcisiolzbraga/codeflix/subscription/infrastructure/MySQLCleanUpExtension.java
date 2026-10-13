package com.tarcisiolzbraga.codeflix.subscription.infrastructure;

import java.util.List;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.junit.jupiter.SpringExtension;

// Esvazia as tabelas antes de cada teste de integração, no lugar de um @BeforeEach por classe.
// Roda antes dos @BeforeEach do próprio teste, então quem semeia dados continua funcionando.
//
// Por SQL, e não por repositório como na admin-api: aqui não há Spring Data, então não existe um
// CrudRepository com deleteAll para chamar.
public class MySQLCleanUpExtension implements BeforeEachCallback {

    // A ordem importa: uma tabela que referencia outra vem antes, por causa das foreign keys.
    // Tabela nova entra nesta lista.
    private static final List<String> TABLES = List.of("plan");

    @Override
    public void beforeEach(final ExtensionContext context) {
        final var jdbcClient = SpringExtension.getApplicationContext(context).getBean(JdbcClient.class);
        TABLES.forEach(table -> jdbcClient.sql("DELETE FROM " + table).update());
    }
}
