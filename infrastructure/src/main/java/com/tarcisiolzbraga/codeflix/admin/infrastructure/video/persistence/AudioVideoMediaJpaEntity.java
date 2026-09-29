package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence;

import com.tarcisiolzbraga.codeflix.admin.domain.video.AudioVideoMedia;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

// A mídia é value object no domínio e não tem id; aqui ele existe só para a linha ter chave. Cada
// novo envio gera uma linha nova, e a antiga sai pelo orphanRemoval do vínculo no vídeo.
@Audited
@Entity(name = "AudioVideoMedia")
@Table(name = "audio_video_media")
public class AudioVideoMediaJpaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "checksum", nullable = false)
    private String checksum;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "raw_location", nullable = false, length = 500)
    private String rawLocation;

    @Column(name = "encoded_location", nullable = false, length = 500)
    private String encodedLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_status", nullable = false, length = 20)
    private MediaStatus mediaStatus;

    protected AudioVideoMediaJpaEntity() {
    }

    private AudioVideoMediaJpaEntity(final AudioVideoMedia media) {
        this.id = UUID.randomUUID().toString();
        this.checksum = media.checksum();
        this.name = media.name();
        this.rawLocation = media.rawLocation();
        this.encodedLocation = media.encodedLocation();
        this.mediaStatus = media.status();
    }

    public static AudioVideoMediaJpaEntity from(final AudioVideoMedia media) {
        return media == null ? null : new AudioVideoMediaJpaEntity(media);
    }

    public AudioVideoMedia toDomain() {
        return AudioVideoMedia.with(
                this.checksum, this.name, this.rawLocation, this.encodedLocation, this.mediaStatus);
    }

    public String getId() {
        return this.id;
    }
}
