-- O plano que o assinante escolhe.
--
-- DATETIME(6), e não TIMESTAMP(6) como na referência: o TIMESTAMP do MySQL converte pelo fuso da
-- sessão e para em 2038, e o que se guarda aqui é Instant em UTC. É também o tipo que a admin-api usa.
--
-- version é INT, não SMALLINT: é o contador do lock otimista, e sobe a cada gravação do plano. Com
-- SMALLINT o teto são 32767 gravações da mesma linha, e dois bytes não valem esse limite.
--
-- Os limites das colunas de texto são os mesmos que o agregado valida, para o banco recusar o que o
-- domínio recusaria caso alguma escrita não passe por ele.
CREATE TABLE plan (
    id          CHAR(36)      NOT NULL PRIMARY KEY,
    version     INT           NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    description VARCHAR(500)  NOT NULL,
    active      BOOLEAN       NOT NULL,
    currency    CHAR(3)       NOT NULL,
    amount      DECIMAL(15,2) NOT NULL,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL
);
