package com.tarcisiolzbraga.codeflix.admin.application.castmember.create;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import io.vavr.control.Either;
import java.util.Objects;

public class DefaultCreateCastMemberUseCase extends CreateCastMemberUseCase {

    private final CastMemberGateway castMemberGateway;

    public DefaultCreateCastMemberUseCase(final CastMemberGateway castMemberGateway) {
        this.castMemberGateway = Objects.requireNonNull(castMemberGateway, "'castMemberGateway' should not be null");
    }

    @Override
    public Either<Notification, CreateCastMemberOutput> execute(final CreateCastMemberCommand input) {
        // Texto desconhecido vira tipo nulo, e quem reclama é o validador do domínio: assim o campo
        // ausente e o valor inválido recebem a mesma mensagem, uma só vez.
        final var type = CastMemberType.of(input.type()).orElse(null);
        final var castMember = CastMember.newCastMember(input.name(), type, input.isActive());
        final var notification = Notification.create();
        castMember.validate(notification);

        if (notification.hasError()) {
            return Left(notification);
        }
        return Right(CreateCastMemberOutput.from(this.castMemberGateway.create(castMember)));
    }
}
