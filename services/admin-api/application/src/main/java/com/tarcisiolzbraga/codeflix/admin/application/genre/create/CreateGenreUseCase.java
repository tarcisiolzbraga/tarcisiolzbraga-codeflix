package com.tarcisiolzbraga.codeflix.admin.application.genre.create;

import com.tarcisiolzbraga.codeflix.admin.application.UseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import io.vavr.control.Either;

public abstract class CreateGenreUseCase
        extends UseCase<CreateGenreCommand, Either<Notification, CreateGenreOutput>> {
}
