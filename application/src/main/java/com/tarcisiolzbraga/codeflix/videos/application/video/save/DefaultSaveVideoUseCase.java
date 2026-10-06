package com.tarcisiolzbraga.codeflix.videos.application.video.save;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Video;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoDetails;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoFlags;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoMedias;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoReferences;
import java.time.Year;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultSaveVideoUseCase extends SaveVideoUseCase {

    private static final ValidationError NULL_ID = new ValidationError("'id' should not be null");

    private final VideoGateway videoGateway;

    public DefaultSaveVideoUseCase(final VideoGateway videoGateway) {
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
    }

    @Override
    public SaveVideoOutput execute(final SaveVideoCommand input) {
        Objects.requireNonNull(input, "'input' should not be null");
        if (input.id() == null) {
            throw DomainException.with(NULL_ID);
        }

        final var video = toVideo(input);
        final var notification = Notification.create();
        video.validate(notification);
        if (notification.hasError()) {
            throw DomainException.with(notification.getErrors());
        }

        return SaveVideoOutput.from(this.videoGateway.save(video));
    }

    private static Video toVideo(final SaveVideoCommand input) {
        return Video.with(
                VideoID.from(input.id()),
                detailsOf(input.details()),
                flagsOf(input.flags()),
                mediasOf(input.medias()),
                referencesOf(input.references()),
                input.createdAt(),
                input.updatedAt());
    }

    // Ano e classificação desconhecidos entram como nulo e o validador os reporta, para os erros da
    // mesma mensagem aparecerem juntos em vez de o primeiro virar exceção na conversão.
    private static VideoDetails detailsOf(final VideoDetailsCommand details) {
        if (details == null) {
            return VideoDetails.with(null, null, null, 0, null);
        }
        return VideoDetails.with(
                details.title(),
                details.description(),
                details.launchedAt() == null ? null : Year.of(details.launchedAt()),
                details.duration(),
                Rating.of(details.rating()).orElse(null));
    }

    private static VideoFlags flagsOf(final VideoFlagsCommand flags) {
        if (flags == null) {
            return new VideoFlags(false, false, false);
        }
        return new VideoFlags(flags.opened(), flags.published(), flags.active());
    }

    private static VideoMedias mediasOf(final VideoMediasCommand medias) {
        if (medias == null) {
            return VideoMedias.none();
        }
        return VideoMedias.with(
                medias.video(), medias.trailer(), medias.banner(), medias.thumbnail(), medias.thumbnailHalf());
    }

    // Relações nulas entram vazias, não como erro: vídeo sem categoria é válido no admin, e derrubar
    // o consumo por isso seria pior que replicar sem vínculo, que volta no evento seguinte.
    private static VideoReferences referencesOf(final VideoReferencesCommand references) {
        if (references == null) {
            return VideoReferences.none();
        }
        return VideoReferences.with(
                idsOf(references.categories(), CategoryID::from),
                idsOf(references.genres(), GenreID::from),
                idsOf(references.castMembers(), CastMemberID::from));
    }

    private static <T> Set<T> idsOf(final Set<String> values, final Function<String, T> factory) {
        if (values == null) {
            return Set.of();
        }
        return values.stream().filter(Objects::nonNull).map(factory).collect(Collectors.toUnmodifiableSet());
    }
}
