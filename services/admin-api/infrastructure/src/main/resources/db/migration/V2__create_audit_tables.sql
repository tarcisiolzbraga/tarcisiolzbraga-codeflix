CREATE TABLE revinfo (
    rev      INT    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    revtstmp BIGINT NULL
);

CREATE TABLE category_aud (
    id          CHAR(36)      NOT NULL,
    rev         INT           NOT NULL,
    revtype     TINYINT       NULL,
    name        VARCHAR(255)  NULL,
    description VARCHAR(4000) NULL,
    active      BOOLEAN       NULL,
    created_at  DATETIME(6)   NULL,
    updated_at  DATETIME(6)   NULL,
    deleted_at  DATETIME(6)   NULL,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_category_aud_revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);
