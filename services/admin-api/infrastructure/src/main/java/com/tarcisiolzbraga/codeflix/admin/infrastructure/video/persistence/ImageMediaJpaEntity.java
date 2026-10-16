package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence;

import com.tarcisiolzbraga.codeflix.admin.domain.video.ImageMedia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Audited
@Entity(name = "ImageMedia")
@Table(name = "image_media")
public class ImageMediaJpaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36)
    private UUID id;

    @Column(name = "checksum", nullable = false)
    private String checksum;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "location", nullable = false, length = 500)
    private String location;

    protected ImageMediaJpaEntity() {
    }

    private ImageMediaJpaEntity(final ImageMedia media) {
        this.id = UUID.randomUUID();
        this.checksum = media.checksum();
        this.name = media.name();
        this.location = media.location();
    }

    public static ImageMediaJpaEntity from(final ImageMedia media) {
        return media == null ? null : new ImageMediaJpaEntity(media);
    }

    public ImageMedia toDomain() {
        return ImageMedia.with(this.checksum, this.name, this.location);
    }

    public UUID getId() {
        return this.id;
    }
}
