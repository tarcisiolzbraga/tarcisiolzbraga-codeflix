-- A fila de saída: todo evento de domínio é gravado aqui, na mesma transação do agregado que o
-- produziu.
--
-- É o que faz "gravou e avisou" ser atômico. Publicar direto num broker depois do commit perde o
-- evento se o processo morrer no meio, e publicar antes do commit avisa de algo que pode não ter
-- acontecido. Com a fila, ou as duas escritas acontecem ou nenhuma, e quem leva o evento adiante é
-- o Debezium lendo o binlog — já depois de confirmado.
--
-- **Sem coluna de processado**, ao contrário da referência. Lá o consumidor lê a linha de volta pelo
-- id e a marca, o que custa uma ida ao banco por mensagem e abre uma corrida entre dois consumidores
-- lendo a mesma linha não processada. Aqui a mensagem do CDC já carrega a linha inteira, o consumidor
-- age direto, e o que garante a repetição ser inofensiva é o handler ser idempotente — conceder ou
-- revogar grupo no Keycloak pode acontecer duas vezes sem efeito. Quem guarda o progresso é o offset
-- do Kafka, que é feito para isso.
--
-- Sem chave estrangeira para agregado nenhum, de propósito: o fato aconteceu, e continua verdadeiro
-- mesmo que a assinatura seja apagada depois. Uma chave aqui faria a remoção de um agregado apagar
-- o histórico dele, ou impedi-la.
--
-- Nada de ordenar por `id`: ele é UUID, como todo id do monorepo, e ordem não é assunto dele. A
-- ordem dos eventos é a do binlog, e é o conector que a preserva — com uma partição só, que é como
-- o `provisioning/cdc` já configura.
CREATE TABLE event (
    id             CHAR(36)     NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(32)  NOT NULL,
    aggregate_id   CHAR(36)     NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    occurred_on    DATETIME(6)  NOT NULL,
    payload        JSON         NOT NULL
);

-- Para a pergunta que se faz de verdade quando algo deu errado: o que aconteceu com este agregado.
CREATE INDEX idx_event_aggregate ON event (aggregate_id, occurred_on);
