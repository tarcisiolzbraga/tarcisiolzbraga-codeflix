package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.util.Objects;
import org.hibernate.envers.Audited;

// Vínculo mapeado como entidade, e não @ElementCollection, para ter controle da tabela caso ela
// ganhe colunas próprias.
@Audited
@Entity(name = "VideoCastMember")
@Table(name = "video_cast_member")
public class VideoCastMemberJpaEntity {

    @EmbeddedId
    private VideoCastMemberID id;

    @ManyToOne
    @MapsId("videoId")
    private VideoJpaEntity video;

    protected VideoCastMemberJpaEntity() {
    }

    private VideoCastMemberJpaEntity(final VideoJpaEntity video, final CastMemberID castMemberId) {
        this.id = VideoCastMemberID.from(video.getId(), castMemberId.getValue());
        this.video = video;
    }

    public static VideoCastMemberJpaEntity from(final VideoJpaEntity video, final CastMemberID castMemberId) {
        return new VideoCastMemberJpaEntity(video, castMemberId);
    }

    public VideoCastMemberID getId() {
        return this.id;
    }

    public VideoJpaEntity getVideo() {
        return this.video;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Objects.equals(this.id, ((VideoCastMemberJpaEntity) other).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
