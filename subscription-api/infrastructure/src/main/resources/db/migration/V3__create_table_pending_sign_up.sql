-- O cadastro que espera a pessoa confirmar o e-mail. A conta só nasce na confirmação, então é aqui
-- que o nome e o documento ficam nesse meio-tempo.
--
-- Não há coluna de senha: ela foi direto ao provedor de identidade na hora do cadastro.
--
-- `token_hash` é o resumo SHA-256 em hexadecimal, 64 caracteres, e é único: é por ele que a
-- confirmação encontra a linha. O token em claro não é gravado em lugar nenhum — ele vai no e-mail e
-- mais nada. Assim um vazamento de leitura deste banco não entrega confirmação de cadastro alheio.
--
-- `email` não é único de propósito: pedir o cadastro de novo é legítimo — o e-mail pode não ter
-- chegado —, e quem garante que só um link fica válido por vez é o caso de uso, apagando os
-- pendentes não confirmados do mesmo e-mail antes de gravar o novo.
CREATE TABLE pending_sign_up (
    id              CHAR(36)     NOT NULL PRIMARY KEY,
    version         INT          NOT NULL,
    idp_user_id     VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    firstname       VARCHAR(255) NOT NULL,
    lastname        VARCHAR(255) NOT NULL,
    document_number VARCHAR(14)  NOT NULL,
    document_type   VARCHAR(10)  NOT NULL,
    token_hash      CHAR(64)     NOT NULL UNIQUE,
    expires_at      DATETIME(6)  NOT NULL,
    confirmed_at    DATETIME(6)
);

CREATE INDEX idx_pending_sign_up_email ON pending_sign_up (email);
