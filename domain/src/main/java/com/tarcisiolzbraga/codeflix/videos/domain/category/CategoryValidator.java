package com.tarcisiolzbraga.codeflix.videos.domain.category;

import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationHandler;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.Validator;

public class CategoryValidator extends Validator {

    private static final int NAME_MIN_LENGTH = 3;
    private static final int NAME_MAX_LENGTH = 255;
    private static final String NAME_LENGTH_MESSAGE =
            "'name' must be between %d and %d characters".formatted(NAME_MIN_LENGTH, NAME_MAX_LENGTH);

    private final Category category;

    public CategoryValidator(final Category category, final ValidationHandler handler) {
        super(handler);
        this.category = category;
    }

    @Override
    public void validate() {
        checkIdConstraints();
        checkNameConstraints();
    }

    // O id não é gerado aqui: ele chega pela mensagem do admin-codeflix, e uma mensagem corrompida
    // pode trazê-lo vazio. O construtor do CategoryID barra o nulo, não o branco.
    private void checkIdConstraints() {
        if (this.category.getId().getValue().isBlank()) {
            validationHandler().append(new ValidationError("'id' should not be empty"));
        }
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
}
