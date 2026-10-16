package com.tarcisiolzbraga.codeflix.videos.application.category.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.application.category.CategoryOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.category.Category;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCategoriesByIdUseCaseTest {

    private static final CategoryID FIRST_ID = CategoryID.from("00000001-0000-0000-0000-000000000000");
    private static final CategoryID SECOND_ID = CategoryID.from("00000002-0000-0000-0000-000000000000");
    private static final Instant EXPECTED_CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant EXPECTED_UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    @Mock
    private CategoryGateway categoryGateway;

    @InjectMocks
    private DefaultGetCategoriesByIdUseCase useCase;

    @Test
    void givenKnownIds_whenCallExecute_thenReturnOneOutputPerCategoryFound() {
        final var ids = Set.of(FIRST_ID, SECOND_ID);
        when(categoryGateway.findAllById(ids))
                .thenReturn(List.of(aCategory(FIRST_ID, "Filmes"), aCategory(SECOND_ID, "Séries")));

        final var actualOutput = useCase.execute(ids);

        assertEquals(2, actualOutput.size());
        assertEquals(List.of("Filmes", "Séries"), actualOutput.stream().map(CategoryOutput::name).toList());
        assertEquals(List.of("00000001-0000-0000-0000-000000000000", "00000002-0000-0000-0000-000000000000"), actualOutput.stream().map(CategoryOutput::id).toList());
    }

    @Test
    void givenNoId_whenCallExecute_thenReturnEmptyWithoutTouchingTheGateway() {
        final var actualOutput = useCase.execute(Set.of());

        assertTrue(actualOutput.isEmpty());
        verify(categoryGateway, never()).findAllById(any());
    }

    @Test
    void givenIdsThatTheCatalogDoesNotHave_whenCallExecute_thenReturnOnlyWhatWasFound() {
        final var ids = Set.of(FIRST_ID, SECOND_ID);
        when(categoryGateway.findAllById(ids)).thenReturn(List.of(aCategory(FIRST_ID, "Filmes")));

        final var actualOutput = useCase.execute(ids);

        assertEquals(1, actualOutput.size());
        assertEquals("00000001-0000-0000-0000-000000000000", actualOutput.getFirst().id());
    }

    @Test
    void givenNullInput_whenCallExecute_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> useCase.execute(null));

        assertEquals("'input' should not be null", actualException.getMessage());
        verify(categoryGateway, never()).findAllById(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var ids = Set.of(FIRST_ID);
        final var expectedException = new IllegalStateException("catálogo indisponível");
        when(categoryGateway.findAllById(ids)).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(ids));

        assertSame(expectedException, actualException);
    }

    private static Category aCategory(final CategoryID id, final String name) {
        return Category.with(
                id,
                name,
                "A categoria %s".formatted(name),
                true,
                EXPECTED_CREATED_AT,
                EXPECTED_UPDATED_AT);
    }
}
