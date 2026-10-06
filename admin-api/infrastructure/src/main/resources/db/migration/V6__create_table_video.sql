CREATE TABLE video (
    id            CHAR(36)      NOT NULL PRIMARY KEY,
    title         VARCHAR(255)  NOT NULL,
    description   VARCHAR(4000) NOT NULL,
    year_launched SMALLINT      NOT NULL,
    duration      DECIMAL(6, 2) NOT NULL,
    rating        VARCHAR(5)    NOT NULL,
    opened        BOOLEAN       NOT NULL DEFAULT FALSE,
    published     BOOLEAN       NOT NULL DEFAULT FALSE,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL
);

-- Sem ON DELETE CASCADE: o Envers só audita o que o Hibernate executa. Categoria, gênero ou membro
-- de elenco vinculado a vídeo não é removido, só desativado; o vídeo desfaz os próprios vínculos.
CREATE TABLE video_category (
    video_id    CHAR(36) NOT NULL,
    category_id CHAR(36) NOT NULL,
    PRIMARY KEY (video_id, category_id),
    CONSTRAINT fk_video_category_video FOREIGN KEY (video_id) REFERENCES video (id),
    CONSTRAINT fk_video_category_category FOREIGN KEY (category_id) REFERENCES category (id)
);

CREATE TABLE video_genre (
    video_id CHAR(36) NOT NULL,
    genre_id CHAR(36) NOT NULL,
    PRIMARY KEY (video_id, genre_id),
    CONSTRAINT fk_video_genre_video FOREIGN KEY (video_id) REFERENCES video (id),
    CONSTRAINT fk_video_genre_genre FOREIGN KEY (genre_id) REFERENCES genre (id)
);

CREATE TABLE video_cast_member (
    video_id       CHAR(36) NOT NULL,
    cast_member_id CHAR(36) NOT NULL,
    PRIMARY KEY (video_id, cast_member_id),
    CONSTRAINT fk_video_cast_member_video FOREIGN KEY (video_id) REFERENCES video (id),
    CONSTRAINT fk_video_cast_member_member FOREIGN KEY (cast_member_id) REFERENCES cast_member (id)
);

CREATE TABLE video_aud (
    id            CHAR(36)      NOT NULL,
    rev           INT           NOT NULL,
    revtype       TINYINT       NULL,
    title         VARCHAR(255)  NULL,
    description   VARCHAR(4000) NULL,
    year_launched SMALLINT      NULL,
    duration      DECIMAL(6, 2) NULL,
    rating        VARCHAR(5)    NULL,
    opened        BOOLEAN       NULL,
    published     BOOLEAN       NULL,
    active        BOOLEAN       NULL,
    created_at    DATETIME(6)   NULL,
    updated_at    DATETIME(6)   NULL,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_video_aud_revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE video_category_aud (
    video_id    CHAR(36) NOT NULL,
    category_id CHAR(36) NOT NULL,
    rev         INT      NOT NULL,
    revtype     TINYINT  NULL,
    PRIMARY KEY (video_id, category_id, rev),
    CONSTRAINT fk_video_category_aud_revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE video_genre_aud (
    video_id CHAR(36) NOT NULL,
    genre_id CHAR(36) NOT NULL,
    rev      INT      NOT NULL,
    revtype  TINYINT  NULL,
    PRIMARY KEY (video_id, genre_id, rev),
    CONSTRAINT fk_video_genre_aud_revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE video_cast_member_aud (
    video_id       CHAR(36) NOT NULL,
    cast_member_id CHAR(36) NOT NULL,
    rev            INT      NOT NULL,
    revtype        TINYINT  NULL,
    PRIMARY KEY (video_id, cast_member_id, rev),
    CONSTRAINT fk_video_cast_member_aud_revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);
