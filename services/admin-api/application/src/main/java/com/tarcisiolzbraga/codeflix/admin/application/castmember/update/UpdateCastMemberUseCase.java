package com.tarcisiolzbraga.codeflix.admin.application.castmember.update;

import com.tarcisiolzbraga.codeflix.admin.application.UseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import io.vavr.control.Either;

public abstract class UpdateCastMemberUseCase
        extends UseCase<UpdateCastMemberCommand, Either<Notification, UpdateCastMemberOutput>> {
}
