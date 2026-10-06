package com.tarcisiolzbraga.codeflix.admin.domain.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import org.junit.jupiter.api.Test;

class ConflictExceptionTest {

    @Test
    void givenLinkedAggregate_whenCallLinked_thenBuildMessageWithBothAggregateNames() {
        final var expectedId = CategoryID.unique();

        final var exception = ConflictException.linked(Category.class, expectedId, Genre.class);

        assertEquals(
                "Category with ID %s is linked to at least one Genre; deactivate it instead"
                        .formatted(expectedId.getValue()),
                exception.getMessage());
        assertTrue(exception.getErrors().isEmpty());
        assertEquals(0, exception.getStackTrace().length);
    }
}
