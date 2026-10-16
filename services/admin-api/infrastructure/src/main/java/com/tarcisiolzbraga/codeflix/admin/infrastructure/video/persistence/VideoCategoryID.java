package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
public class VideoCategoryID implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "video_id", length = 36, nullable = false)
    private UUID videoId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "category_id", length = 36, nullable = false)
    private UUID categoryId;

    protected VideoCategoryID() {
    }

    private VideoCategoryID(final UUID videoId, final UUID categoryId) {
        this.videoId = videoId;
        this.categoryId = categoryId;
    }

    public static VideoCategoryID from(final UUID videoId, final UUID categoryId) {
        return new VideoCategoryID(videoId, categoryId);
    }

    public UUID getVideoId() {
        return this.videoId;
    }

    public UUID getCategoryId() {
        return this.categoryId;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final var that = (VideoCategoryID) other;
        return Objects.equals(this.videoId, that.videoId) && Objects.equals(this.categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.videoId, this.categoryId);
    }
}
