package com.tarcisiolzbraga.codeflix.admin.application.castmember.update;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import io.vavr.control.Either;
import java.util.Objects;

public class DefaultUpdateCastMemberUseCase extends UpdateCastMemberUseCase {

    private final CastMemberGateway castMemberGateway;

    public DefaultUpdateCastMemberUseCase(final CastMemberGateway castMemberGateway) {
        this.castMemberGateway = Objects.requireNonNull(castMemberGateway, "'castMemberGateway' should not be null");
    }

    @Override
    public Either<Notification, UpdateCastMemberOutput> execute(final UpdateCastMemberCommand input) {
        final var castMember = findById(CastMemberID.from(input.id()));
        final var type = CastMemberType.of(input.type()).orElse(null);
        final var notification = Notification.create();
        castMember.update(input.name(), type).validate(notification);

        if (notification.hasError()) {
            return Left(notification);
        }
        return Right(UpdateCastMemberOutput.from(this.castMemberGateway.update(castMember)));
    }

    private CastMember findById(final CastMemberID id) {
        return this.castMemberGateway
                .findById(id)
                .orElseThrow(() -> NotFoundException.with(CastMember.class, id));
    }
}
