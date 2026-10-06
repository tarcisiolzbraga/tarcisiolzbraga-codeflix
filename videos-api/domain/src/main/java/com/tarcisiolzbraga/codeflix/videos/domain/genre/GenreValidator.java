package com.tarcisiolzbraga.codeflix.videos.domain.genre;

import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationHandler;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.Validator;

public class GenreValidator extends Validator {

    private static final int NAME_MIN_LENGTH = 3;
    private static final int NAME_MAX_LENGTH = 255;
    private static final String NAME_LENGTH_MESSAGE =
            "'name' must be between %d and %d characters".formatted(NAME_MIN_LENGTH, NAME_MAX_LENGTH);

    private final Genre genre;

    public GenreValidator(final Genre genre, final ValidationHandler handler) {
        super(handler);
        this.genre = genre;
    }

    @Override
    public void validate() {
        checkIdConstraints();
        checkNameConstraints();
    }

    // O id chega pela mensagem do admin-codeflix, que pode vir corrompida; o construtor do GenreID
    // barra o nulo, não o branco.
    private void checkIdConstraints() {
        if (this.genre.getId().getValue().isBlank()) {
            validationHandler().append(new ValidationError("'id' should not be empty"));
        }
    }

    private void checkNameConstraints() {
        final var name = this.genre.getName();
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
