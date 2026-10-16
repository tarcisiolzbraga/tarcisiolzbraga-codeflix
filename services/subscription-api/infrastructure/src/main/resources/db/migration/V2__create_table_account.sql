-- Quem assina. A conta só nasce depois de o e-mail ser confirmado, então não há coluna de estado de
-- validação: a linha existir já significa que o e-mail foi conferido.
--
-- Sem data alguma, diferente de `plan`: nada na conta se decide por tempo.
--
-- `document_number` cabe o maior documento aceito (CNPJ, 14 dígitos) e `document_type` os nomes dos
-- tipos. A referência usa VARCHAR(255) nos dois; aqui a coluna recusa o que o domínio recusaria.
--
-- Sem índice próprio em `idp_user_id`: o UNIQUE já cria um no MySQL, e a referência declara os dois.
CREATE TABLE account (
    id                 CHAR(36)     NOT NULL PRIMARY KEY,
    version            INT          NOT NULL,
    idp_user_id        VARCHAR(255) NOT NULL UNIQUE,
    email              VARCHAR(255) NOT NULL UNIQUE,
    firstname          VARCHAR(255) NOT NULL,
    lastname           VARCHAR(255) NOT NULL,
    document_number    VARCHAR(14)  NOT NULL,
    document_type      VARCHAR(10)  NOT NULL,
    address_zipcode    VARCHAR(255),
    address_number     VARCHAR(255),
    address_complement VARCHAR(255),
    address_country    VARCHAR(255),

    -- O endereço de cobrança é tudo ou nada: ou as quatro colunas estão vazias, ou as três
    -- obrigatórias estão preenchidas. É a mesma regra do value object Address, dita onde o banco
    -- consegue afirmá-la — nenhuma escrita que não passe pelo domínio deixa meio endereço gravado.
    CONSTRAINT ck_account_billing_address CHECK (
        (address_zipcode IS NULL AND address_number IS NULL
            AND address_complement IS NULL AND address_country IS NULL)
        OR (address_zipcode IS NOT NULL AND address_number IS NOT NULL AND address_country IS NOT NULL)
    )
);
