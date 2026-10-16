-- A assinatura de uma conta num plano.
--
-- Com chave estrangeira para as duas: assinatura de conta que não existe, ou de plano que não
-- existe, é dado quebrado, e o banco é o único lugar que consegue garantir isso sob concorrência.
--
-- Sem ON DELETE CASCADE, como no resto do monorepo: apagar conta ou plano por baixo da assinatura
-- apagaria histórico de cobrança sem ninguém decidir. A chave segura, e quem quiser remover resolve
-- a assinatura primeiro.
--
-- `due_date` é DATE e não DATETIME: o vencimento é um dia, não um instante — é o que decide se a
-- cobrança de hoje acontece.
CREATE TABLE subscription (
    id                  CHAR(36)     NOT NULL PRIMARY KEY,
    version             INT          NOT NULL,
    account_id          CHAR(36)     NOT NULL,
    plan_id             CHAR(36)     NOT NULL,
    status              VARCHAR(16)  NOT NULL,
    due_date            DATE         NOT NULL,
    last_renew_date     DATETIME(6),
    last_transaction_id VARCHAR(255),
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,

    CONSTRAINT fk_subscription_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES plan (id)
);
