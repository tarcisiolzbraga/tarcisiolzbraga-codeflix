package com.tarcisiolzbraga.codeflix.admin.application.castmember.create;

import com.tarcisiolzbraga.codeflix.admin.application.UseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import io.vavr.control.Either;

public abstract class CreateCastMemberUseCase
        extends UseCase<CreateCastMemberCommand, Either<Notification, CreateCastMemberOutput>> {
}
