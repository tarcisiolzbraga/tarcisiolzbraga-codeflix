package com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// A fachada do JdbcClient: um lugar só por onde o SQL deste serviço passa.
//
// Só o que algum repositório usa. Duas coisas da referência ficaram de fora:
//
// - o `insert` com GeneratedKeyHolder, que existia para ler a chave que o auto-increment sorteava.
//   Aqui o id é UUID gerado no domínio, então inserir é um update como qualquer outro.
// O `query` que devolve List entrou com a varredura das vencidas, e com LIMIT e OFFSET no SQL de
// quem o chama — devolver coleção sem limite vinda do banco é proibido no monorepo. A fachada não
// impõe o limite porque ela não escreve o SQL; quem o escreve é o repositório, e é lá que a regra
// vale.
public interface DatabaseClient {

    <T> Optional<T> queryOne(String sql, Map<String, Object> params, RowMap<T> mapper);

    <T> List<T> query(String sql, Map<String, Object> params, RowMap<T> mapper);

    int update(String sql, Map<String, Object> params);
}
