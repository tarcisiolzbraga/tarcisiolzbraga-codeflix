package com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

// O RowMapper do Spring sem o rowNum, que nenhum mapeamento daqui usa. É o que o repositório
// implementa para ler o ResultSet direto para dentro do agregado.
@FunctionalInterface
public interface RowMap<T> {

    T mapRow(ResultSet rs) throws SQLException;
}
