package com.tarcisiolzbraga.codeflix.admin.application.category.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateCategoryUseCaseTest {

    private static final String EXPECTED_NAME = "Filmes";
    private static final String EXPECTED_DESCRIPTION = "A categoria mais assistida";

    @Mock
    private CategoryGateway categoryGateway;

    @InjectMocks
    private DefaultUpdateCategoryUseCase useCase;

    @Test
    void givenValidCommand_whenCallExecute_thenReturnRightWithUpdatedCategory() {
        final var category = givenStoredCategory(true);
        final var command = commandFor(category, EXPECTED_NAME, true);
        when(categoryGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        assertEquals(category.getId().getValue(), actualResult.get().id());
        verify(categoryGateway).update(argThat(updated -> isUpdatedFrom(updated, category, true)));
    }

    @Test
    void givenInactiveCommand_whenCallExecute_thenDeactivateTheCategory() {
        final var category = givenStoredCategory(true);
        final var command = commandFor(category, EXPECTED_NAME, false);
        when(categoryGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        verify(categoryGateway).update(argThat(updated -> isUpdatedFrom(updated, category, false)));
    }

    @Test
    void givenInactiveCategory_whenCallExecuteWithActiveCommand_thenActivateIt() {
        final var category = givenStoredCategory(false);
        final var command = commandFor(category, EXPECTED_NAME, true);
        when(categoryGateway.update(any())).thenAnswer(returnsFirstArg());

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isRight());
        verify(categoryGateway).update(argThat(updated -> isUpdatedFrom(updated, category, true)));
    }

    @Test
    void givenNullName_whenCallExecute_thenReturnLeftWithNotificationAndNotUpdate() {
        final var category = givenStoredCategory(true);
        final var command = commandFor(category, null, true);

        final var actualResult = useCase.execute(command);

        assertTrue(actualResult.isLeft());
        assertEquals(1, actualResult.getLeft().getErrors().size());
        assertEquals("'name' should not be null", firstErrorOf(actualResult.getLeft()));
        verify(categoryGateway, never()).update(any());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFoundAndNotUpdate() {
        final var expectedId = CategoryID.unique();
        final var command = UpdateCategoryCommand.with(expectedId.getValue(), EXPECTED_NAME, EXPECTED_DESCRIPTION, true);
        when(categoryGateway.findById(expectedId)).thenReturn(Optional.empty());

        final var actualException = assertThrows(NotFoundException.class, () -> useCase.execute(command));

        assertEquals(
                "Category with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
        verify(categoryGateway, never()).update(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var category = givenStoredCategory(true);
        final var command = commandFor(category, EXPECTED_NAME, true);
        final var expectedException = new IllegalStateException("gateway indisponível");
        when(categoryGateway.update(any())).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        assertSame(expectedException, actualException);
        verify(categoryGateway).update(any());
    }

    private Category givenStoredCategory(final boolean isActive) {
        final var category = Category.newCategory("Série", "A categoria antiga", isActive);
        when(categoryGateway.findById(category.getId())).thenReturn(Optional.of(category));
        return category;
    }

    private UpdateCategoryCommand commandFor(final Category category, final String name, final boolean isActive) {
        return UpdateCategoryCommand.with(category.getId().getValue(), name, EXPECTED_DESCRIPTION, isActive);
    }

    private String firstErrorOf(final Notification notification) {
        return notification.firstError().orElseThrow().message();
    }

    private boolean isUpdatedFrom(final Category updated, final Category stored, final boolean isActive) {
        final var hasExpectedFields = EXPECTED_NAME.equals(updated.getName())
                && EXPECTED_DESCRIPTION.equals(updated.getDescription())
                && updated.isActive() == isActive
                && updated.getId().equals(stored.getId())
                && updated.getCreatedAt().equals(stored.getCreatedAt());
        return hasExpectedFields && (isActive == (updated.getDeletedAt() == null));
    }
}
