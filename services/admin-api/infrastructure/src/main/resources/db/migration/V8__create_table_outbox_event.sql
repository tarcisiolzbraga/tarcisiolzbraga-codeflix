-- Fila de saída: o evento é gravado aqui na mesma transação do agregado, e um relay entrega ao
-- broker depois. Assim gravar e avisar deixam de ser dois passos que podem divergir.
--
-- Sem tabela _aud: isto é mecanismo de entrega, não agregado. O que aconteceu com o vídeo já está
-- auditado em video_aud; duplicar esse histórico aqui só encheria o banco.
CREATE TABLE outbox_event (
    id          CHAR(36)     NOT NULL PRIMARY KEY,
    routing_key VARCHAR(255) NOT NULL,
    payload     TEXT         NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    sent_at     DATETIME(6)  NULL
);

-- O relay procura sempre o mesmo: o que ainda não foi entregue, na ordem em que entrou.
CREATE INDEX idx_outbox_event_pending ON outbox_event (sent_at, created_at);
