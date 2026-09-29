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
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

public class Video extends AggregateRoot<VideoID> {

    private static final String DETAILS_NOT_NULL_MESSAGE = "'details' should not be null";
    private static final String REFERENCES_NOT_NULL_MESSAGE = "'references' should not be null";
    private static final String MEDIA_NOT_NULL_MESSAGE = "'media' should not be null";

    private VideoDetails details;
    // Referência a outro agregado só pelo ID, como no Genre.
    private VideoReferences references;
    private boolean opened;
    private boolean published;
    private AudioVideoMedia video;
    private AudioVideoMedia trailer;
    private ImageMedia banner;
    private ImageMedia thumbnail;
    private ImageMedia thumbnailHalf;

    private Video(
            final VideoID id,
            final VideoDetails details,
            final VideoReferences references,
            final VideoMedias medias,
            final VideoFlags flags,
            final Instant createdAt,
            final Instant updatedAt) {
        super(id, flags.active(), createdAt, updatedAt);
        this.details = Objects.requireNonNull(details, DETAILS_NOT_NULL_MESSAGE);
        this.references = Objects.requireNonNull(references, REFERENCES_NOT_NULL_MESSAGE);
        this.opened = flags.opened();
        this.published = flags.published();
        this.video = medias.video();
        this.trailer = medias.trailer();
        this.banner = medias.banner();
        this.thumbnail = medias.thumbnail();
        this.thumbnailHalf = medias.thumbnailHalf();
    }

    // Nasce fechado e não publicado: abrir e publicar são decisões próprias, com método para cada uma.
    public static Video newVideo(final VideoDetails details, final VideoReferences references) {
        final var now = InstantUtils.now();
        return new Video(
                VideoID.unique(),
                details,
                references,
                VideoMedias.none(),
                VideoFlags.closedAndUnpublished(),
                now,
                now);
    }

    public static Video with(
            final VideoID id,
            final VideoDetails details,
            final VideoReferences references,
            final VideoMedias medias,
            final VideoFlags flags,
            final Instant createdAt,
            final Instant updatedAt) {
        return new Video(id, details, references, medias, flags, createdAt, updatedAt);
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

    // Um método por arquivo, como manda a regra de um método de intenção por regra: o caso de uso
    // escolhe qual chamar a partir do tipo recebido na rota. Só áudio e vídeo avisam o mundo de
    // fora: imagem não passa por codificação, então não há o que o codificador faça com ela.
    public void updateVideoMedia(final AudioVideoMedia media) {
        this.video = Objects.requireNonNull(media, MEDIA_NOT_NULL_MESSAGE);
        registerEvent(new VideoMediaCreated(getId().getValue(), VideoMediaType.VIDEO, media.rawLocation()));
        refreshUpdatedAt();
    }

    public void updateTrailerMedia(final AudioVideoMedia media) {
        this.trailer = Objects.requireNonNull(media, MEDIA_NOT_NULL_MESSAGE);
        registerEvent(new VideoMediaCreated(getId().getValue(), VideoMediaType.TRAILER, media.rawLocation()));
        refreshUpdatedAt();
    }

    public void updateBanner(final ImageMedia media) {
        this.banner = Objects.requireNonNull(media, MEDIA_NOT_NULL_MESSAGE);
        refreshUpdatedAt();
    }

    public void updateThumbnail(final ImageMedia media) {
        this.thumbnail = Objects.requireNonNull(media, MEDIA_NOT_NULL_MESSAGE);
        refreshUpdatedAt();
    }

    public void updateThumbnailHalf(final ImageMedia media) {
        this.thumbnailHalf = Objects.requireNonNull(media, MEDIA_NOT_NULL_MESSAGE);
        refreshUpdatedAt();
    }

    // O retorno do codificador chega pela fila e pode falar de um arquivo que já foi trocado ou
    // removido, ou de uma imagem, que não é codificada: nesses casos não há o que mover, e falhar
    // só faria a mensagem voltar para sempre.
    public void processing(final VideoMediaType type) {
        applyToAudioVideo(type, AudioVideoMedia::processing);
    }

    public void completed(final VideoMediaType type, final String encodedLocation) {
        applyToAudioVideo(type, media -> media.completed(encodedLocation));
    }

    private void applyToAudioVideo(final VideoMediaType type, final UnaryOperator<AudioVideoMedia> change) {
        switch (type) {
            case VIDEO -> this.video = moved(this.video, change);
            case TRAILER -> this.trailer = moved(this.trailer, change);
            default -> {
                // imagem não tem status
            }
        }
    }

    private AudioVideoMedia moved(final AudioVideoMedia media, final UnaryOperator<AudioVideoMedia> change) {
        if (media == null) {
            return null;
        }
        refreshUpdatedAt();
        return change.apply(media);
    }

    public Optional<AudioVideoMedia> getVideo() {
        return Optional.ofNullable(this.video);
    }

    public Optional<AudioVideoMedia> getTrailer() {
        return Optional.ofNullable(this.trailer);
    }

    public Optional<ImageMedia> getBanner() {
        return Optional.ofNullable(this.banner);
    }

    public Optional<ImageMedia> getThumbnail() {
        return Optional.ofNullable(this.thumbnail);
    }

    public Optional<ImageMedia> getThumbnailHalf() {
        return Optional.ofNullable(this.thumbnailHalf);
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
