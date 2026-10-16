package com.tarcisiolzbraga.codeflix.admin.domain.video;

import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.Validator;

public class VideoValidator extends Validator {

    private static final int TITLE_MAX_LENGTH = 255;
    private static final int DESCRIPTION_MAX_LENGTH = 4000;
    private static final String TITLE_LENGTH_MESSAGE =
            "'title' must be between 1 and %d characters".formatted(TITLE_MAX_LENGTH);
    private static final String DESCRIPTION_LENGTH_MESSAGE =
            "'description' must be between 1 and %d characters".formatted(DESCRIPTION_MAX_LENGTH);

    private final Video video;

    public VideoValidator(final Video video, final ValidationHandler handler) {
        super(handler);
        this.video = video;
    }

    @Override
    public void validate() {
        checkTitleConstraints();
        checkDescriptionConstraints();
        checkDurationConstraints();
        checkLaunchedAtConstraints();
        checkRatingConstraints();
    }

    private void checkTitleConstraints() {
        checkTextConstraints(this.video.getTitle(), "title", TITLE_MAX_LENGTH, TITLE_LENGTH_MESSAGE);
    }

    private void checkDescriptionConstraints() {
        checkTextConstraints(
                this.video.getDescription(), "description", DESCRIPTION_MAX_LENGTH, DESCRIPTION_LENGTH_MESSAGE);
    }

    private void checkTextConstraints(
            final String value, final String field, final int maxLength, final String lengthMessage) {
        if (value == null) {
            validationHandler().append(new ValidationError("'%s' should not be null".formatted(field)));
            return;
        }
        if (value.isBlank()) {
            validationHandler().append(new ValidationError("'%s' should not be empty".formatted(field)));
            return;
        }
        // O mínimo é 1 caractere, já garantido pela checagem de texto em branco acima.
        if (value.trim().length() > maxLength) {
            validationHandler().append(new ValidationError(lengthMessage));
        }
    }

    private void checkDurationConstraints() {
        if (this.video.getDuration() < 0) {
            validationHandler().append(new ValidationError("'duration' should not be negative"));
        }
    }

    private void checkLaunchedAtConstraints() {
        if (this.video.getLaunchedAt() == null) {
            validationHandler().append(new ValidationError("'launchedAt' should not be null"));
        }
    }

    private void checkRatingConstraints() {
        if (this.video.getRating() == null) {
            validationHandler().append(new ValidationError("'rating' should not be null"));
        }
    }
}
