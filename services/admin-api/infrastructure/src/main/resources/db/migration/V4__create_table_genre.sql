CREATE TABLE genre (
    id         CHAR(36)     NOT NULL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL
);

-- Sem ON DELETE CASCADE: o Envers só audita o que o Hibernate executa. Categoria vinculada a gênero
-- não é removida, só desativada; o gênero desfaz os próprios vínculos, registrados em genre_category_aud.
CREATE TABLE genre_category (
    genre_id    CHAR(36) NOT NULL,
    category_id CHAR(36) NOT NULL,
    PRIMARY KEY (genre_id, category_id),
    CONSTRAINT fk_genre_category_genre FOREIGN KEY (genre_id) REFERENCES genre (id),
    CONSTRAINT fk_genre_category_category FOREIGN KEY (category_id) REFERENCES category (id)
);

CREATE TABLE genre_aud (
    id         CHAR(36)     NOT NULL,
    rev        INT          NOT NULL,
    revtype    TINYINT      NULL,
    name       VARCHAR(255) NULL,
    active     BOOLEAN      NULL,
    created_at DATETIME(6)  NULL,
    updated_at DATETIME(6)  NULL,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_genre_aud_revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE genre_category_aud (
    genre_id    CHAR(36) NOT NULL,
    category_id CHAR(36) NOT NULL,
    rev         INT      NOT NULL,
    revtype     TINYINT  NULL,
    PRIMARY KEY (genre_id, category_id, rev),
    CONSTRAINT fk_genre_category_aud_revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);
