package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
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
@Entity(name = "VideoCategory")
@Table(name = "video_category")
public class VideoCategoryJpaEntity {

    @EmbeddedId
    private VideoCategoryID id;

    @ManyToOne
    @MapsId("videoId")
    private VideoJpaEntity video;

    protected VideoCategoryJpaEntity() {
    }

    private VideoCategoryJpaEntity(final VideoJpaEntity video, final CategoryID categoryId) {
        this.id = VideoCategoryID.from(video.getId(), categoryId.getValue());
        this.video = video;
    }

    public static VideoCategoryJpaEntity from(final VideoJpaEntity video, final CategoryID categoryId) {
        return new VideoCategoryJpaEntity(video, categoryId);
    }

    public VideoCategoryID getId() {
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
        return Objects.equals(this.id, ((VideoCategoryJpaEntity) other).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
