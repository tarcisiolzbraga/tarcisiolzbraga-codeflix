package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence;

import com.tarcisiolzbraga.codeflix.admin.domain.Identifier;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoDetails;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoFlags;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoReferences;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.persistence.BaseJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Year;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Audited
@Entity(name = "Video")
@Table(name = "video")
public class VideoJpaEntity extends BaseJpaEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    // A coluna é SMALLINT, que basta para um ano; sem isto o Hibernate esperaria INTEGER.
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "year_launched", nullable = false)
    private int yearLaunched;

    // A coluna é DECIMAL(6,2); sem isto o Hibernate esperaria FLOAT para um double.
    @JdbcTypeCode(SqlTypes.DECIMAL)
    @Column(name = "duration", nullable = false, precision = 6, scale = 2)
    private double duration;

    @Column(name = "rating", nullable = false, length = 5)
    private Rating rating;

    @Column(name = "opened", nullable = false)
    private boolean opened;

    @Column(name = "published", nullable = false)
    private boolean published;

    // EAGER porque o agregado sempre é reconstruído inteiro; na listagem, o batch fetch do
    // Hibernate carrega as coleções em lotes, sem N+1 e sem paginar em memória.
    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<VideoCategoryJpaEntity> categories = new HashSet<>();

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<VideoGenreJpaEntity> genres = new HashSet<>();

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<VideoCastMemberJpaEntity> castMembers = new HashSet<>();

    protected VideoJpaEntity() {
    }

    private VideoJpaEntity(final Video video) {
        super(video.getId().getValue(), video.isActive(), video.getCreatedAt(), video.getUpdatedAt());
        this.title = video.getTitle();
        this.description = video.getDescription();
        this.yearLaunched = video.getLaunchedAt() == null ? 0 : video.getLaunchedAt().getValue();
        this.duration = video.getDuration();
        this.rating = video.getRating();
        this.opened = video.isOpened();
        this.published = video.isPublished();
        video.getCategories().forEach(id -> this.categories.add(VideoCategoryJpaEntity.from(this, id)));
        video.getGenres().forEach(id -> this.genres.add(VideoGenreJpaEntity.from(this, id)));
        video.getCastMembers().forEach(id -> this.castMembers.add(VideoCastMemberJpaEntity.from(this, id)));
    }

    public static VideoJpaEntity from(final Video video) {
        return new VideoJpaEntity(video);
    }

    public Video toAggregate() {
        return Video.with(
                VideoID.from(getId()),
                VideoDetails.with(this.title, this.description, Year.of(this.yearLaunched), this.duration, this.rating),
                VideoReferences.with(getCategoryIds(), getGenreIds(), getCastMemberIds()),
                new VideoFlags(this.opened, this.published, isActive()),
                getCreatedAt(),
                getUpdatedAt());
    }

    public String getTitle() {
        return this.title;
    }

    public Rating getRating() {
        return this.rating;
    }

    public boolean isOpened() {
        return this.opened;
    }

    public boolean isPublished() {
        return this.published;
    }

    public Set<CategoryID> getCategoryIds() {
        return idsOf(this.categories, link -> CategoryID.from(link.getId().getCategoryId()));
    }

    public Set<GenreID> getGenreIds() {
        return idsOf(this.genres, link -> GenreID.from(link.getId().getGenreId()));
    }

    public Set<CastMemberID> getCastMemberIds() {
        return idsOf(this.castMembers, link -> CastMemberID.from(link.getId().getCastMemberId()));
    }

    private <LINK, ID extends Identifier> Set<ID> idsOf(final Set<LINK> links, final Function<LINK, ID> factory) {
        return links.stream().map(factory).collect(Collectors.toUnmodifiableSet());
    }
}
