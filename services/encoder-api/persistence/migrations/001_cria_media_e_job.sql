-- O que o encoder guarda: a mídia que recebeu e cada tentativa de convertê-la.
--
-- Migration explícita, e não AutoMigrate como no curso: o esquema é revisado como código, e a
-- regra daqui é que mudança de schema seja migration — nunca o framework decidindo sozinho o que
-- alterar numa base que já tem dado.

CREATE TABLE media (
    id         UUID         NOT NULL PRIMARY KEY,
    video_id   UUID         NOT NULL,
    type       VARCHAR(16)  NOT NULL,
    checksum   VARCHAR(255) NOT NULL,
    file_path  VARCHAR(512) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);

-- O par (video_id, type, checksum) é o que identifica um envio: reenviar o mesmo tipo sobrescreve
-- o arquivo no mesmo caminho, e é o checksum que distingue um do outro. O índice serve à pergunta
-- "já converti este envio?", que é como o trabalho repetido é evitado.
CREATE UNIQUE INDEX idx_media_envio ON media (video_id, type, checksum);

CREATE TABLE job (
    id                 UUID         NOT NULL PRIMARY KEY,
    media_id           UUID         NOT NULL REFERENCES media (id),
    output_bucket_path VARCHAR(512) NOT NULL,
    status             VARCHAR(16)  NOT NULL,
    error              TEXT         NOT NULL DEFAULT '',
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL
);

-- A pergunta que se faz ao job é sempre pela mídia: qual foi o último trabalho dela.
CREATE INDEX idx_job_media ON job (media_id, created_at DESC);
