package com.tarcisiolzbraga.codeflix.admin.domain.castmember;

import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.Validator;
import java.util.stream.Stream;

public class CastMemberValidator extends Validator {

    private static final int NAME_MIN_LENGTH = 3;
    private static final int NAME_MAX_LENGTH = 255;
    private static final String NAME_LENGTH_MESSAGE =
            "'name' must be between %d and %d characters".formatted(NAME_MIN_LENGTH, NAME_MAX_LENGTH);
    // Uma mensagem só serve ao campo ausente e ao valor desconhecido: em ambos os casos o que o
    // cliente precisa saber é quais tipos existem.
    private static final String TYPE_MESSAGE = "'type' should be one of %s"
            .formatted(String.join(", ", Stream.of(CastMemberType.values()).map(Enum::name).toList()));

    private final CastMember castMember;

    public CastMemberValidator(final CastMember castMember, final ValidationHandler handler) {
        super(handler);
        this.castMember = castMember;
    }

    @Override
    public void validate() {
        checkNameConstraints();
        checkTypeConstraints();
    }

    private void checkNameConstraints() {
        final var name = this.castMember.getName();
        if (name == null) {
            validationHandler().append(new ValidationError("'name' should not be null"));
            return;
        }
        if (name.isBlank()) {
            validationHandler().append(new ValidationError("'name' should not be empty"));
            return;
        }
        final var length = name.trim().length();
        if (length < NAME_MIN_LENGTH || length > NAME_MAX_LENGTH) {
            validationHandler().append(new ValidationError(NAME_LENGTH_MESSAGE));
        }
    }

    private void checkTypeConstraints() {
        if (this.castMember.getType() == null) {
            validationHandler().append(new ValidationError(TYPE_MESSAGE));
        }
    }
}
