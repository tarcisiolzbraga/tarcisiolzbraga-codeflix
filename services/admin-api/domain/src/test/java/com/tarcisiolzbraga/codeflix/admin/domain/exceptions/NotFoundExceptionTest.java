package com.tarcisiolzbraga.codeflix.admin.domain.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import org.junit.jupiter.api.Test;

class NotFoundExceptionTest {

    @Test
    void givenAggregateAndId_whenCallWith_thenBuildMessageWithTheAggregateName() {
        final var expectedId = CategoryID.unique();

        final var exception = NotFoundException.with(Category.class, expectedId);

        assertEquals("Category with ID %s was not found".formatted(expectedId.getValue()), exception.getMessage());
        assertTrue(exception.getErrors().isEmpty());
        assertEquals(0, exception.getStackTrace().length);
    }

    @Test
    void givenMediaTypeAndVideoId_whenCallWithMedia_thenSayWhichMediaOfWhichVideo() {
        final var expectedId = VideoID.unique();

        final var exception = NotFoundException.withMedia("BANNER", expectedId);

        assertEquals(
                "Media BANNER of Video with ID %s was not found".formatted(expectedId.getValue()),
                exception.getMessage());
        assertTrue(exception.getErrors().isEmpty());
    }
}
