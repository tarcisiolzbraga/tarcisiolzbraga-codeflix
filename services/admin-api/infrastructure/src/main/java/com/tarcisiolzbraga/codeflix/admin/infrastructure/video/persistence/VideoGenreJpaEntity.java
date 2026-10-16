package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence;

import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
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
@Entity(name = "VideoGenre")
@Table(name = "video_genre")
public class VideoGenreJpaEntity {

    @EmbeddedId
    private VideoGenreID id;

    @ManyToOne
    @MapsId("videoId")
    private VideoJpaEntity video;

    protected VideoGenreJpaEntity() {
    }

    private VideoGenreJpaEntity(final VideoJpaEntity video, final GenreID genreId) {
        this.id = VideoGenreID.from(video.getId(), genreId.value());
        this.video = video;
    }

    public static VideoGenreJpaEntity from(final VideoJpaEntity video, final GenreID genreId) {
        return new VideoGenreJpaEntity(video, genreId);
    }

    public VideoGenreID getId() {
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
        return Objects.equals(this.id, ((VideoGenreJpaEntity) other).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
