package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.api;

import static io.vavr.API.Left;
import static io.vavr.API.Right;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarcisiolzbraga.codeflix.admin.application.category.CategoryOutput;
import com.tarcisiolzbraga.codeflix.admin.application.category.activate.ActivateCategoryUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.create.CreateCategoryOutput;
import com.tarcisiolzbraga.codeflix.admin.application.category.create.CreateCategoryUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.deactivate.DeactivateCategoryUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.delete.DeleteCategoryUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.get.GetCategoryByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.list.CategoryListOutput;
import com.tarcisiolzbraga.codeflix.admin.application.category.list.ListCategoriesUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.category.update.UpdateCategoryOutput;
import com.tarcisiolzbraga.codeflix.admin.application.category.update.UpdateCategoryUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.ControllerTest;
import jakarta.servlet.ServletException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(controllers = CategoryController.class)
class CategoryControllerTest {

    private static final String CATEGORIES_PATH = "/categories";
    private static final String EXPECTED_ID = "11111111-1111-1111-1111-111111111111";
    private static final String EXPECTED_NAME = "Filmes";
    private static final String EXPECTED_DESCRIPTION = "A mais assistida";
    private static final String VALID_BODY =
            """
            {"name":"Filmes","description":"A mais assistida","active":true}""";

    private static final String VALID_UPDATE_BODY =
            """
            {"name":"Filmes","description":"A mais assistida"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCategoryUseCase createCategoryUseCase;

    @MockitoBean
    private GetCategoryByIdUseCase getCategoryByIdUseCase;

    @MockitoBean
    private UpdateCategoryUseCase updateCategoryUseCase;

    @MockitoBean
    private DeleteCategoryUseCase deleteCategoryUseCase;

    @MockitoBean
    private ListCategoriesUseCase listCategoriesUseCase;

    @MockitoBean
    private ActivateCategoryUseCase activateCategoryUseCase;

    @MockitoBean
    private DeactivateCategoryUseCase deactivateCategoryUseCase;

    @Test
    void givenValidBody_whenCallCreate_thenReturn201WithLocation() throws Exception {
        when(createCategoryUseCase.execute(any())).thenReturn(Right(new CreateCategoryOutput(EXPECTED_ID)));

        final var response = this.mockMvc.perform(
                post(CATEGORIES_PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY));

        response.andExpect(status().isCreated())
                .andExpect(header().string("Location", CATEGORIES_PATH + "/" + EXPECTED_ID))
                .andExpect(jsonPath("$.id").value(EXPECTED_ID));
        verify(createCategoryUseCase).execute(argThat(command -> EXPECTED_NAME.equals(command.name())
                && EXPECTED_DESCRIPTION.equals(command.description())
                && command.isActive()));
    }

    @Test
    void givenInvalidName_whenCallCreate_thenReturn422WithErrors() throws Exception {
        final var notification = Notification.create(new ValidationError("'name' should not be null"));
        when(createCategoryUseCase.execute(any())).thenReturn(Left(notification));

        final var response = this.mockMvc.perform(post(CATEGORIES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"description":"A mais assistida","active":true}"""));

        response.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("'name' should not be null"))
                .andExpect(jsonPath("$.errors[0]").value("'name' should not be null"));
    }

    @Test
    void givenValidId_whenCallGetById_thenReturn200WithTheCategory() throws Exception {
        final var category = Category.newCategory("Filmes", "A mais assistida", true);
        when(getCategoryByIdUseCase.execute(EXPECTED_ID)).thenReturn(CategoryOutput.from(category));

        final var response = this.mockMvc.perform(get(CATEGORIES_PATH + "/" + EXPECTED_ID));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(category.getId().getValue()))
                .andExpect(jsonPath("$.name").value("Filmes"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void givenUnknownId_whenCallGetById_thenReturn404() throws Exception {
        when(getCategoryByIdUseCase.execute(EXPECTED_ID))
                .thenThrow(NotFoundException.with(Category.class, CategoryID.from(EXPECTED_ID)));

        final var response = this.mockMvc.perform(get(CATEGORIES_PATH + "/" + EXPECTED_ID));

        response.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category with ID %s was not found".formatted(EXPECTED_ID)));
    }

    // Falha de infraestrutura não é erro de domínio: o advice deixa passar. Sem container de
    // verdade o MockMvc relança a exceção em vez de montar o 500, e é isso que se confere aqui.
    @Test
    void givenFailingUseCase_whenCallGetById_thenDoNotHandleItAsDomainError() {
        final var expectedException = new IllegalStateException("gateway indisponível");
        when(getCategoryByIdUseCase.execute(EXPECTED_ID)).thenThrow(expectedException);

        final var actualException = assertThrows(
                ServletException.class, () -> this.mockMvc.perform(get(CATEGORIES_PATH + "/" + EXPECTED_ID)));

        assertSame(expectedException, actualException.getCause());
    }

    @Test
    void givenSearchParams_whenCallList_thenReturn200WithPagination() throws Exception {
        final var category = Category.newCategory("Filmes", "A mais assistida", true);
        final var page = new Pagination<>(1, 5, 1L, List.of(CategoryListOutput.from(category)));
        when(listCategoriesUseCase.execute(new SearchQuery(1, 5, "fil", "description", "desc")))
                .thenReturn(page);

        final var response = this.mockMvc.perform(get(CATEGORIES_PATH)
                .queryParam("search", "fil")
                .queryParam("page", "1")
                .queryParam("perPage", "5")
                .queryParam("sort", "description")
                .queryParam("dir", "desc"));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Filmes"));
    }

    @Test
    void givenNoSearchParam_whenCallList_thenUseTheDefaults() throws Exception {
        when(listCategoriesUseCase.execute(new SearchQuery(0, 10, null, "name", "asc")))
                .thenReturn(new Pagination<>(0, 10, 0L, List.of()));

        final var response = this.mockMvc.perform(get(CATEGORIES_PATH));

        response.andExpect(status().isOk()).andExpect(jsonPath("$.perPage").value(10));
    }

    @Test
    void givenValidBody_whenCallUpdate_thenReturn200() throws Exception {
        when(updateCategoryUseCase.execute(any())).thenReturn(Right(new UpdateCategoryOutput(EXPECTED_ID)));

        final var response = this.mockMvc.perform(put(CATEGORIES_PATH + "/" + EXPECTED_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_UPDATE_BODY));

        response.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(EXPECTED_ID));
        verify(updateCategoryUseCase).execute(argThat(command -> EXPECTED_ID.equals(command.id())
                && EXPECTED_NAME.equals(command.name())
                && EXPECTED_DESCRIPTION.equals(command.description())));
    }

    @Test
    void givenInvalidName_whenCallUpdate_thenReturn422WithErrors() throws Exception {
        final var notification = Notification.create(new ValidationError("'name' should not be empty"));
        when(updateCategoryUseCase.execute(any())).thenReturn(Left(notification));

        final var response = this.mockMvc.perform(put(CATEGORIES_PATH + "/" + EXPECTED_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"   ","description":"A mais assistida"}"""));

        response.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0]").value("'name' should not be empty"));
    }

    @Test
    void givenValidId_whenCallActivate_thenReturn200WithTheActiveCategory() throws Exception {
        final var category = Category.newCategory("Filmes", "A mais assistida", true);
        when(activateCategoryUseCase.execute(EXPECTED_ID)).thenReturn(CategoryOutput.from(category));

        final var response = this.mockMvc.perform(put(CATEGORIES_PATH + "/" + EXPECTED_ID + "/activate"));

        response.andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void givenValidId_whenCallDeactivate_thenReturn200WithTheInactiveCategory() throws Exception {
        final var category = Category.newCategory("Filmes", "A mais assistida", false);
        when(deactivateCategoryUseCase.execute(EXPECTED_ID)).thenReturn(CategoryOutput.from(category));

        final var response = this.mockMvc.perform(put(CATEGORIES_PATH + "/" + EXPECTED_ID + "/deactivate"));

        response.andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void givenValidId_whenCallDelete_thenReturn204() throws Exception {
        final var response = this.mockMvc.perform(delete(CATEGORIES_PATH + "/" + EXPECTED_ID));

        response.andExpect(status().isNoContent());
        verify(deleteCategoryUseCase).execute(EXPECTED_ID);
    }

    @Test
    void givenCategoryLinkedToGenre_whenCallDelete_thenReturn409() throws Exception {
        doThrow(ConflictException.linked(Category.class, CategoryID.from(EXPECTED_ID), Genre.class))
                .when(deleteCategoryUseCase).execute(EXPECTED_ID);

        final var response = this.mockMvc.perform(delete(CATEGORIES_PATH + "/" + EXPECTED_ID));

        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Category with ID %s is linked to at least one Genre; deactivate it instead"
                                .formatted(EXPECTED_ID)))
                .andExpect(jsonPath("$.errors").isEmpty());
    }
}
