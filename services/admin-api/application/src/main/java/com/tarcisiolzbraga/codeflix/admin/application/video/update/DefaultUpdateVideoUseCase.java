package com.tarcisiolzbraga.codeflix.admin.application.video.update;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoReferenceExistence;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import io.vavr.control.Either;
import java.util.Objects;

public class DefaultUpdateVideoUseCase extends UpdateVideoUseCase {

    private final VideoGateway videoGateway;
    private final VideoReferenceExistence referenceExistence;

    public DefaultUpdateVideoUseCase(
            final CategoryGateway categoryGateway,
            final GenreGateway genreGateway,
            final CastMemberGateway castMemberGateway,
            final VideoGateway videoGateway) {
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
        this.referenceExistence = new VideoReferenceExistence(categoryGateway, genreGateway, castMemberGateway);
    }

    @Override
    public Either<Notification, UpdateVideoOutput> execute(final UpdateVideoCommand input) {
        final var video = findById(VideoID.from(input.id()));
        final var notification = Notification.create();
        final var references = this.referenceExistence.validate(input.references(), notification);
        video.update(input.fields().toDetails(), references).validate(notification);

        if (notification.hasError()) {
            return Left(notification);
        }
        return Right(UpdateVideoOutput.from(this.videoGateway.update(video)));
    }

    private Video findById(final VideoID id) {
        return this.videoGateway.findById(id).orElseThrow(() -> NotFoundException.with(Video.class, id));
    }
}
