package com.tarcisiolzbraga.codeflix.admin.application.category.update;

import com.tarcisiolzbraga.codeflix.admin.application.UseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import io.vavr.control.Either;

public abstract class UpdateCategoryUseCase
        extends UseCase<UpdateCategoryCommand, Either<Notification, UpdateCategoryOutput>> {
}
