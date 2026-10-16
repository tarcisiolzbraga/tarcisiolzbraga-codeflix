package com.tarcisiolzbraga.codeflix.admin.application.video.create;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoReferenceExistence;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import io.vavr.control.Either;
import java.util.Objects;

public class DefaultCreateVideoUseCase extends CreateVideoUseCase {

    private final VideoGateway videoGateway;
    private final VideoReferenceExistence referenceExistence;

    public DefaultCreateVideoUseCase(
            final CategoryGateway categoryGateway,
            final GenreGateway genreGateway,
            final CastMemberGateway castMemberGateway,
            final VideoGateway videoGateway) {
        this.videoGateway = Objects.requireNonNull(videoGateway, "'videoGateway' should not be null");
        this.referenceExistence = new VideoReferenceExistence(categoryGateway, genreGateway, castMemberGateway);
    }

    @Override
    public Either<Notification, CreateVideoOutput> execute(final CreateVideoCommand input) {
        final var notification = Notification.create();
        final var references = this.referenceExistence.validate(input.references(), notification);
        final var video = Video.newVideo(input.fields().toDetails(), references);
        video.validate(notification);

        if (notification.hasError()) {
            return Left(notification);
        }
        return Right(CreateVideoOutput.from(this.videoGateway.create(video)));
    }
}
