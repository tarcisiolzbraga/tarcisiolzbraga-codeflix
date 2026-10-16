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
public class VideoCastMemberID implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "video_id", length = 36, nullable = false)
    private UUID videoId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cast_member_id", length = 36, nullable = false)
    private UUID castMemberId;

    protected VideoCastMemberID() {
    }

    private VideoCastMemberID(final UUID videoId, final UUID castMemberId) {
        this.videoId = videoId;
        this.castMemberId = castMemberId;
    }

    public static VideoCastMemberID from(final UUID videoId, final UUID castMemberId) {
        return new VideoCastMemberID(videoId, castMemberId);
    }

    public UUID getVideoId() {
        return this.videoId;
    }

    public UUID getCastMemberId() {
        return this.castMemberId;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final var that = (VideoCastMemberID) other;
        return Objects.equals(this.videoId, that.videoId) && Objects.equals(this.castMemberId, that.castMemberId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.videoId, this.castMemberId);
    }
}
