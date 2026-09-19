package com.tarcisiolzbraga.codeflix.admin.application.category.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateCategoryUseCaseTest {

    private static final String EXPECTED_NAME = "Filmes";
    private static final String EXPECTED_DESCRIPTION = "A categoria mais assistida";

    @Mock
    private CategoryGateway categoryGateway;

    @InjectMocks
    private DefaultCreateCategoryUseCase useCase;

    @Test
    void givenValidCommand_whenCallExecute_thenReturnRightWithCategoryId() {
        final var command = CreateCategoryCommand.with(EXPECTED_NAME, EXPECTED_DESCRIPTION, true);
        when(categoryGateway.create(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        assertNotNull(actualResult.get().id());
        verify(categoryGateway).create(argThat(category -> isCreatedFrom(category, true)));
    }

    @Test
    void givenInactiveCommand_whenCallExecute_thenCreateDeletedCategory() {
        final var command = CreateCategoryCommand.with(EXPECTED_NAME, EXPECTED_DESCRIPTION, false);
        when(categoryGateway.create(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        verify(categoryGateway).create(argThat(category -> isCreatedFrom(category, false)));
    }

    @Test
    void givenNullName_whenCallExecute_thenReturnLeftWithNotificationAndNotCreate() {
        final var command = CreateCategoryCommand.with(null, EXPECTED_DESCRIPTION, true);

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isLeft());
        assertEquals(1, actualResult.getLeft().getErrors().size());
        assertEquals("'name' should not be null", firstErrorOf(actualResult.getLeft()));
        verify(categoryGateway, never()).create(any());
    }

    @Test
    void givenBlankName_whenCallExecute_thenReturnLeftWithNotificationAndNotCreate() {
        final var command = CreateCategoryCommand.with("   ", EXPECTED_DESCRIPTION, true);

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isLeft());
        assertEquals("'name' should not be empty", firstErrorOf(actualResult.getLeft()));
        verify(categoryGateway, never()).create(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var command = CreateCategoryCommand.with(EXPECTED_NAME, EXPECTED_DESCRIPTION, true);
        final var expectedException = new IllegalStateException("gateway indisponível");
        when(categoryGateway.create(any())).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        assertSame(expectedException, actualException);
        verify(categoryGateway).create(any());
    }

    private String firstErrorOf(final Notification notification) {
        return notification.firstError().orElseThrow().message();
    }

    private boolean isCreatedFrom(final Category category, final boolean isActive) {
        final var hasExpectedFields = EXPECTED_NAME.equals(category.getName())
                && EXPECTED_DESCRIPTION.equals(category.getDescription())
                && category.isActive() == isActive
                && category.getId() != null
                && category.getCreatedAt() != null
                && category.getUpdatedAt() != null;
        return hasExpectedFields && (isActive == (category.getDeletedAt() == null));
    }
}
