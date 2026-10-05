package com.tarcisiolzbraga.codeflix.videos.application.castmember.save;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.handler.Notification;
import java.util.Objects;

// Guarda no catálogo a versão que o admin-codeflix publicou, como o caso de uso da categoria: erro
// de validação sobe como exceção, não como Either, porque ninguém digitou estes dados.
public class DefaultSaveCastMemberUseCase extends SaveCastMemberUseCase {

    private static final ValidationError NULL_ID = new ValidationError("'id' should not be null");

    private final CastMemberGateway castMemberGateway;

    public DefaultSaveCastMemberUseCase(final CastMemberGateway castMemberGateway) {
        this.castMemberGateway =
                Objects.requireNonNull(castMemberGateway, "'castMemberGateway' should not be null");
    }

    @Override
    public SaveCastMemberOutput execute(final SaveCastMemberCommand input) {
        Objects.requireNonNull(input, "'input' should not be null");
        if (input.id() == null) {
            throw DomainException.with(NULL_ID);
        }

        final var castMember = toCastMember(input);
        final var notification = Notification.create();
        castMember.validate(notification);
        if (notification.hasError()) {
            throw DomainException.with(notification.getErrors());
        }

        return SaveCastMemberOutput.from(this.castMemberGateway.save(castMember));
    }

    // Tipo desconhecido entra como nulo e o validador o reporta nomeando os que existem; converter
    // com exceção aqui perderia os outros erros da mesma mensagem.
    private static CastMember toCastMember(final SaveCastMemberCommand input) {
        return CastMember.with(
                CastMemberID.from(input.id()),
                input.name(),
                CastMemberType.of(input.type()).orElse(null),
                input.active(),
                input.createdAt(),
                input.updatedAt());
    }
}
