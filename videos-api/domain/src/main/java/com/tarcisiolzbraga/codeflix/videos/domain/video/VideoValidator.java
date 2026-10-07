package com.tarcisiolzbraga.codeflix.videos.domain.video;

import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationHandler;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.Validator;

public class VideoValidator extends Validator {

    private static final int TITLE_MIN_LENGTH = 3;
    private static final int TITLE_MAX_LENGTH = 255;
    private static final int DESCRIPTION_MAX_LENGTH = 4000;
    private static final String TITLE_LENGTH_MESSAGE =
            "'title' must be between %d and %d characters".formatted(TITLE_MIN_LENGTH, TITLE_MAX_LENGTH);
    private static final String DESCRIPTION_LENGTH_MESSAGE =
            "'description' must be at most %d characters".formatted(DESCRIPTION_MAX_LENGTH);

    private final Video video;

    public VideoValidator(final Video video, final ValidationHandler handler) {
        super(handler);
        this.video = video;
    }

    @Override
    public void validate() {
        checkTitleConstraints();
        checkDescriptionConstraints();
        checkLaunchedAtConstraints();
        checkDurationConstraints();
        checkRatingConstraints();
    }

    private void checkTitleConstraints() {
        final var title = this.video.getDetails().title();
        if (title == null) {
            validationHandler().append(new ValidationError("'title' should not be null"));
            return;
        }
        if (title.isBlank()) {
            validationHandler().append(new ValidationError("'title' should not be empty"));
            return;
        }
        final var length = title.trim().length();
        if (length < TITLE_MIN_LENGTH || length > TITLE_MAX_LENGTH) {
            validationHandler().append(new ValidationError(TITLE_LENGTH_MESSAGE));
        }
    }

    // A descrição é obrigatória na tabela do admin, então um nulo aqui é mensagem corrompida.
    private void checkDescriptionConstraints() {
        final var description = this.video.getDetails().description();
        if (description == null) {
            validationHandler().append(new ValidationError("'description' should not be null"));
            return;
        }
        if (description.trim().length() > DESCRIPTION_MAX_LENGTH) {
            validationHandler().append(new ValidationError(DESCRIPTION_LENGTH_MESSAGE));
        }
    }

    private void checkLaunchedAtConstraints() {
        if (this.video.getDetails().launchedAt() == null) {
            validationHandler().append(new ValidationError("'launchedAt' should not be null"));
        }
    }

    private void checkDurationConstraints() {
        if (this.video.getDetails().duration() < 0) {
            validationHandler().append(new ValidationError("'duration' should not be negative"));
        }
    }

    // Nulo cobre o rótulo desconhecido também: quem converte devolve nulo quando não reconhece, para
    // o erro aparecer aqui junto dos outros da mesma mensagem em vez de virar exceção.
    private void checkRatingConstraints() {
        if (this.video.getDetails().rating() == null) {
            validationHandler().append(new ValidationError("'rating' should not be null"));
        }
    }
}
