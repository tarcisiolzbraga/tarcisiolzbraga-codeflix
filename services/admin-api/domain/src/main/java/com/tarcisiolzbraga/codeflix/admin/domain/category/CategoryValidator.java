package com.tarcisiolzbraga.codeflix.admin.domain.category;

import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.Validator;

public class CategoryValidator extends Validator {

    private static final int NAME_MIN_LENGTH = 3;
    private static final int NAME_MAX_LENGTH = 255;
    private static final String NAME_LENGTH_MESSAGE =
            "'name' must be between %d and %d characters".formatted(NAME_MIN_LENGTH, NAME_MAX_LENGTH);
    // O mesmo limite da coluna `description`, e o mesmo que o VideoValidator já aplicava à descrição
    // do vídeo. Sem ele, uma descrição mais longa não chegava a ser recusada aqui: ela ia ao banco e
    // voltava como erro de integridade, que a API não mapeia — ou seja, 500 em vez de 422.
    private static final int DESCRIPTION_MAX_LENGTH = 4000;
    private static final String DESCRIPTION_LENGTH_MESSAGE =
            "'description' must be at most %d characters".formatted(DESCRIPTION_MAX_LENGTH);

    private final Category category;

    public CategoryValidator(final Category category, final ValidationHandler handler) {
        super(handler);
        this.category = category;
    }

    @Override
    public void validate() {
        checkNameConstraints();
        checkDescriptionConstraints();
    }

    private void checkNameConstraints() {
        final var name = this.category.getName();
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

    // A descrição é opcional, diferente da do vídeo: nula e em branco são estados válidos, e por isso
    // a única regra é o teto.
    private void checkDescriptionConstraints() {
        final var description = this.category.getDescription();
        if (description != null && description.trim().length() > DESCRIPTION_MAX_LENGTH) {
            validationHandler().append(new ValidationError(DESCRIPTION_LENGTH_MESSAGE));
        }
    }
}
