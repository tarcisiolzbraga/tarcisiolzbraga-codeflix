package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
public class VideoGenreID implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "video_id", length = 36, nullable = false)
    private String videoId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "genre_id", length = 36, nullable = false)
    private String genreId;

    protected VideoGenreID() {
    }

    private VideoGenreID(final String videoId, final String genreId) {
        this.videoId = videoId;
        this.genreId = genreId;
    }

    public static VideoGenreID from(final String videoId, final String genreId) {
        return new VideoGenreID(videoId, genreId);
    }

    public String getVideoId() {
        return this.videoId;
    }

    public String getGenreId() {
        return this.genreId;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final var that = (VideoGenreID) other;
        return Objects.equals(this.videoId, that.videoId) && Objects.equals(this.genreId, that.genreId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.videoId, this.genreId);
    }
}
