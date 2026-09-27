package com.tarcisiolzbraga.codeflix.admin.application.video.update;

import com.tarcisiolzbraga.codeflix.admin.application.UseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import io.vavr.control.Either;

public abstract class UpdateVideoUseCase
        extends UseCase<UpdateVideoCommand, Either<Notification, UpdateVideoOutput>> {
}
