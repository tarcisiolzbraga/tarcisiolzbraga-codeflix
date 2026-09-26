package com.tarcisiolzbraga.codeflix.admin.domain.video;

import com.tarcisiolzbraga.codeflix.admin.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import java.time.Instant;
import java.time.Year;
import java.util.Objects;
import java.util.Set;

public class Video extends AggregateRoot<VideoID> {

    private static final String DETAILS_NOT_NULL_MESSAGE = "'details' should not be null";
    private static final String REFERENCES_NOT_NULL_MESSAGE = "'references' should not be null";

    private VideoDetails details;
    // Referência a outro agregado só pelo ID, como no Genre.
    private VideoReferences references;
    private boolean opened;
    private boolean published;

    private Video(
            final VideoID id,
            final VideoDetails details,
            final VideoReferences references,
            final VideoFlags flags,
            final Instant createdAt,
            final Instant updatedAt) {
        super(id, flags.active(), createdAt, updatedAt);
        this.details = Objects.requireNonNull(details, DETAILS_NOT_NULL_MESSAGE);
        this.references = Objects.requireNonNull(references, REFERENCES_NOT_NULL_MESSAGE);
        this.opened = flags.opened();
        this.published = flags.published();
    }

    // Nasce fechado e não publicado: abrir e publicar são decisões próprias, com método para cada uma.
    public static Video newVideo(final VideoDetails details, final VideoReferences references) {
        final var now = InstantUtils.now();
        return new Video(VideoID.unique(), details, references, VideoFlags.closedAndUnpublished(), now, now);
    }

    public static Video with(
            final VideoID id,
            final VideoDetails details,
            final VideoReferences references,
            final VideoFlags flags,
            final Instant createdAt,
            final Instant updatedAt) {
        return new Video(id, details, references, flags, createdAt, updatedAt);
    }

    @Override
    public void validate(final ValidationHandler handler) {
        new VideoValidator(this, handler).validate();
    }

    public Video update(final VideoDetails details, final VideoReferences references) {
        this.details = Objects.requireNonNull(details, DETAILS_NOT_NULL_MESSAGE);
        this.references = Objects.requireNonNull(references, REFERENCES_NOT_NULL_MESSAGE);
        refreshUpdatedAt();
        return this;
    }

    public void publish() {
        this.published = true;
        refreshUpdatedAt();
    }

    public void unpublish() {
        this.published = false;
        refreshUpdatedAt();
    }

    public void open() {
        this.opened = true;
        refreshUpdatedAt();
    }

    public void close() {
        this.opened = false;
        refreshUpdatedAt();
    }

    public String getTitle() {
        return this.details.title();
    }

    public String getDescription() {
        return this.details.description();
    }

    public Year getLaunchedAt() {
        return this.details.launchedAt();
    }

    public double getDuration() {
        return this.details.duration();
    }

    public Rating getRating() {
        return this.details.rating();
    }

    public boolean isOpened() {
        return this.opened;
    }

    public boolean isPublished() {
        return this.published;
    }

    public Set<CategoryID> getCategories() {
        return this.references.categories();
    }

    public Set<GenreID> getGenres() {
        return this.references.genres();
    }

    public Set<CastMemberID> getCastMembers() {
        return this.references.castMembers();
    }
}
