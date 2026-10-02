package com.tarcisiolzbraga.codeflix.videos.application.category.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.domain.category.Category;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaveCategoryUseCaseTest {

    private static final String EXPECTED_ID = "3f2b1a9c-5d6e-4f70-8a91-b2c3d4e5f607";
    private static final String EXPECTED_NAME = "Filmes";
    private static final String EXPECTED_DESCRIPTION = "A categoria mais assistida";
    private static final Instant EXPECTED_CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant EXPECTED_UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    @Mock
    private CategoryGateway categoryGateway;

    @InjectMocks
    private DefaultSaveCategoryUseCase useCase;

    @Test
    void givenValidCommand_whenCallExecute_thenStoreTheReplicaAndReturnItsId() {
        final var command = aCommand(EXPECTED_NAME);
        when(categoryGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var actualOutput = useCase.execute(command);

        assertEquals(EXPECTED_ID, actualOutput.id());
        final var captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryGateway).save(captor.capture());
        final var actualCategory = captor.getValue();
        assertEquals(EXPECTED_ID, actualCategory.getId().getValue());
        assertEquals(EXPECTED_NAME, actualCategory.getName());
        assertEquals(EXPECTED_DESCRIPTION, actualCategory.getDescription());
        assertEquals(EXPECTED_CREATED_AT, actualCategory.getCreatedAt());
        assertEquals(EXPECTED_UPDATED_AT, actualCategory.getUpdatedAt());
    }

    @Test
    void givenNullId_whenCallExecute_thenThrowDomainExceptionAndNotTouchTheGateway() {
        final var command = new SaveCategoryCommand(
                null, EXPECTED_NAME, EXPECTED_DESCRIPTION, true, EXPECTED_CREATED_AT, EXPECTED_UPDATED_AT);

        final var actualException = assertThrows(DomainException.class, () -> useCase.execute(command));

        assertEquals("'id' should not be null", actualException.getMessage());
        verify(categoryGateway, never()).save(any());
    }

    @Test
    void givenBlankName_whenCallExecute_thenThrowDomainExceptionAndNotTouchTheGateway() {
        final var command = aCommand("  ");

        final var actualException = assertThrows(DomainException.class, () -> useCase.execute(command));

        assertEquals("'name' should not be empty", actualException.getMessage());
        verify(categoryGateway, never()).save(any());
    }

    @Test
    void givenNullCommand_whenCallExecute_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> useCase.execute(null));

        assertEquals("'input' should not be null", actualException.getMessage());
        verify(categoryGateway, never()).save(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var command = aCommand(EXPECTED_NAME);
        final var expectedException = new IllegalStateException("catálogo indisponível");
        when(categoryGateway.save(any())).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        assertSame(expectedException, actualException);
    }

    private static SaveCategoryCommand aCommand(final String name) {
        return new SaveCategoryCommand(
                EXPECTED_ID, name, EXPECTED_DESCRIPTION, true, EXPECTED_CREATED_AT, EXPECTED_UPDATED_AT);
    }
}
