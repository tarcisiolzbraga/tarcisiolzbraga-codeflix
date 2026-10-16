package com.tarcisiolzbraga.codeflix.admin.application.genre.update;

import com.tarcisiolzbraga.codeflix.admin.application.UseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import io.vavr.control.Either;

public abstract class UpdateGenreUseCase
        extends UseCase<UpdateGenreCommand, Either<Notification, UpdateGenreOutput>> {
}
