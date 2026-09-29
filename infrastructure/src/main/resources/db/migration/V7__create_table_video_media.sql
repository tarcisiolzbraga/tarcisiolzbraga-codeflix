-- O arquivo mora no armazenamento; aqui ficam só os dados dele e o endereço de onde está.
CREATE TABLE audio_video_media (
    id               CHAR(36)     NOT NULL PRIMARY KEY,
    checksum         VARCHAR(255) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    raw_location     VARCHAR(500) NOT NULL,
    encoded_location VARCHAR(500) NOT NULL,
    media_status     VARCHAR(20)  NOT NULL
);

-- Imagem não passa por codificação: um endereço só, sem status.
CREATE TABLE image_media (
    id       CHAR(36)     NOT NULL PRIMARY KEY,
    checksum VARCHAR(255) NOT NULL,
    name     VARCHAR(255) NOT NULL,
    location VARCHAR(500) NOT NULL
);

-- O vínculo é do vídeo para a mídia, e sem cascata no banco: apagar um vídeo apaga as mídias dele
-- pelo Hibernate, que é o único jeito de o Envers registrar a remoção.
ALTER TABLE video
    ADD COLUMN video_id           CHAR(36) NULL,
    ADD COLUMN trailer_id         CHAR(36) NULL,
    ADD COLUMN banner_id          CHAR(36) NULL,
    ADD COLUMN thumbnail_id       CHAR(36) NULL,
    ADD COLUMN thumbnail_half_id  CHAR(36) NULL,
    ADD CONSTRAINT fk_video_video_media FOREIGN KEY (video_id) REFERENCES audio_video_media (id),
    ADD CONSTRAINT fk_video_trailer_media FOREIGN KEY (trailer_id) REFERENCES audio_video_media (id),
    ADD CONSTRAINT fk_video_banner_media FOREIGN KEY (banner_id) REFERENCES image_media (id),
    ADD CONSTRAINT fk_video_thumbnail_media FOREIGN KEY (thumbnail_id) REFERENCES image_media (id),
    ADD CONSTRAINT fk_video_thumbnail_half_media FOREIGN KEY (thumbnail_half_id) REFERENCES image_media (id);

CREATE TABLE audio_video_media_aud (
    id               CHAR(36)     NOT NULL,
    rev              INT          NOT NULL,
    revtype          TINYINT      NULL,
    checksum         VARCHAR(255) NULL,
    name             VARCHAR(255) NULL,
    raw_location     VARCHAR(500) NULL,
    encoded_location VARCHAR(500) NULL,
    media_status     VARCHAR(20)  NULL,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_audio_video_media_aud_revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE image_media_aud (
    id       CHAR(36)     NOT NULL,
    rev      INT          NOT NULL,
    revtype  TINYINT      NULL,
    checksum VARCHAR(255) NULL,
    name     VARCHAR(255) NULL,
    location VARCHAR(500) NULL,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_image_media_aud_revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

ALTER TABLE video_aud
    ADD COLUMN video_id          CHAR(36) NULL,
    ADD COLUMN trailer_id        CHAR(36) NULL,
    ADD COLUMN banner_id         CHAR(36) NULL,
    ADD COLUMN thumbnail_id      CHAR(36) NULL,
    ADD COLUMN thumbnail_half_id CHAR(36) NULL;
