package com.tarcisiolzbraga.codeflix.videos.domain.video;

import com.tarcisiolzbraga.codeflix.videos.domain.Entity;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationHandler;
import java.time.Instant;
import java.util.Objects;

// Réplica do vídeo que o admin-codeflix governa, com a mesma forma dos outros agregados daqui:
// imutável, só with(...), sem newX(...) nem métodos que mudem estado.
//
// Os campos vêm agrupados em value objects, como no admin: o catálogo do curso tem uma fábrica de
// dezoito parâmetros, o que não passa do limite de dez deste projeto. Agrupados, são sete.
//
// As três relações são guardadas só por id. Como nas do gênero, a tabela de junção não é capturada
// pelo CDC: video_category, video_genre e video_cast_member ficam fora do conector, e o vínculo só
// chega na resposta da API do admin. Funciona porque o Video de lá chama refreshUpdatedAt() no
// update, que é por onde as relações mudam, tocando a linha do video e gerando o evento.
public final class Video extends Entity<VideoID> {

    private final VideoDetails details;
    private final VideoFlags flags;
    private final VideoMedias medias;
    private final VideoReferences references;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Video(
            final VideoID id,
            final VideoDetails details,
            final VideoFlags flags,
            final VideoMedias medias,
            final VideoReferences references,
            final Instant createdAt,
            final Instant updatedAt) {
        super(id);
        this.details = Objects.requireNonNull(details, "'details' should not be null");
        this.flags = Objects.requireNonNull(flags, "'flags' should not be null");
        this.medias = Objects.requireNonNull(medias, "'medias' should not be null");
        this.references = Objects.requireNonNull(references, "'references' should not be null");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Video with(
            final VideoID id,
            final VideoDetails details,
            final VideoFlags flags,
            final VideoMedias medias,
            final VideoReferences references,
            final Instant createdAt,
            final Instant updatedAt) {
        return new Video(id, details, flags, medias, references, createdAt, updatedAt);
    }

    public static Video with(final Video video) {
        return with(
                video.getId(),
                video.getDetails(),
                video.getFlags(),
                video.getMedias(),
                video.getReferences(),
                video.getCreatedAt(),
                video.getUpdatedAt());
    }

    @Override
    public void validate(final ValidationHandler handler) {
        new VideoValidator(this, handler).validate();
    }

    public VideoDetails getDetails() {
        return this.details;
    }

    public VideoFlags getFlags() {
        return this.flags;
    }

    public VideoMedias getMedias() {
        return this.medias;
    }

    public VideoReferences getReferences() {
        return this.references;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }
}
