package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence;

import com.tarcisiolzbraga.codeflix.admin.domain.video.Rating;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// Guarda o rótulo ("12"), e não o nome da constante ("AGE_12"): é o mesmo valor que a API expõe.
@Converter(autoApply = true)
public class RatingConverter implements AttributeConverter<Rating, String> {

    @Override
    public String convertToDatabaseColumn(final Rating rating) {
        return rating == null ? null : rating.getLabel();
    }

    @Override
    public Rating convertToEntityAttribute(final String label) {
        return Rating.of(label).orElse(null);
    }
}
