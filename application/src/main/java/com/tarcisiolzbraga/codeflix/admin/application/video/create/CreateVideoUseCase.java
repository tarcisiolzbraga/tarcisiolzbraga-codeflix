package com.tarcisiolzbraga.codeflix.admin.application.video.create;

import com.tarcisiolzbraga.codeflix.admin.application.UseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import io.vavr.control.Either;

public abstract class CreateVideoUseCase
        extends UseCase<CreateVideoCommand, Either<Notification, CreateVideoOutput>> {
}
