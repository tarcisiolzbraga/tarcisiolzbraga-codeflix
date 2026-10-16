CREATE TABLE cast_member (
    id         CHAR(36)     NOT NULL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    type       VARCHAR(32)  NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL
);

CREATE TABLE cast_member_aud (
    id         CHAR(36)     NOT NULL,
    rev        INT          NOT NULL,
    revtype    TINYINT      NULL,
    name       VARCHAR(255) NULL,
    type       VARCHAR(32)  NULL,
    active     BOOLEAN      NULL,
    created_at DATETIME(6)  NULL,
    updated_at DATETIME(6)  NULL,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_cast_member_aud_revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);
