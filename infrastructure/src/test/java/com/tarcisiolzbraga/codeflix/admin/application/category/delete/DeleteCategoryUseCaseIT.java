package com.tarcisiolzbraga.codeflix.admin.application.category.delete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class DeleteCategoryUseCaseIT {

    @Autowired
    private DeleteCategoryUseCase useCase;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoSpyBean
    private CategoryGateway categoryGateway;

    @BeforeEach
    void cleanUp() {
        this.categoryRepository.deleteAll();
    }

    @Test
    void givenPersistedCategory_whenCallExecute_thenRemoveIt() {
        final var category = givenPersistedCategory();

        this.useCase.execute(category.getId().getValue());

        assertEquals(0, this.categoryRepository.count());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenDoNothing() {
        givenPersistedCategory();

        this.useCase.execute(CategoryID.unique().getValue());

        assertEquals(1, this.categoryRepository.count());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var category = givenPersistedCategory();
        final var expectedException = new IllegalStateException("banco indisponível");
        doThrow(expectedException).when(this.categoryGateway).deleteById(any());

        final var actualException = assertThrows(
                IllegalStateException.class, () -> this.useCase.execute(category.getId().getValue()));

        assertSame(expectedException, actualException);
        assertEquals(1, this.categoryRepository.count());
    }

    private Category givenPersistedCategory() {
        return this.categoryGateway.create(Category.newCategory("Filmes", "A mais assistida", true));
    }
}
