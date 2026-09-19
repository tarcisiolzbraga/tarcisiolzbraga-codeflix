package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class CategoryControllerIT {

    private static final String CATEGORIES_PATH = "/categories";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanUp() {
        this.categoryRepository.deleteAll();
    }

    @Test
    void givenValidBody_whenCallCreate_thenPersistAndReturn201() throws Exception {
        final var response = this.mockMvc.perform(post(CATEGORIES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Filmes","description":"A mais assistida","active":true}"""));

        response.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNotEmpty());
        assertEquals(1, this.categoryRepository.count());
    }

    @Test
    void givenInvalidName_whenCallCreate_thenReturn422AndPersistNothing() throws Exception {
        final var response = this.mockMvc.perform(post(CATEGORIES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"description":"A mais assistida","active":true}"""));

        response.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0]").value("'name' should not be null"));
        assertEquals(0, this.categoryRepository.count());
    }

    @Test
    void givenPersistedCategory_whenCallGetById_thenReturnIt() throws Exception {
        final var category = givenPersistedCategory("Filmes", true);

        final var response = this.mockMvc.perform(get(CATEGORIES_PATH + "/" + category.getId().getValue()));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Filmes"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void givenUnknownId_whenCallGetById_thenReturn404() throws Exception {
        final var response = this.mockMvc.perform(get(CATEGORIES_PATH + "/nao-existe"));

        response.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category with ID nao-existe was not found"));
    }

    @Test
    void givenPersistedCategories_whenCallList_thenReturnFilteredPagination() throws Exception {
        givenPersistedCategory("Filmes", true);
        givenPersistedCategory("Séries", true);

        final var response = this.mockMvc.perform(get(CATEGORIES_PATH).queryParam("search", "fil"));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Filmes"));
    }

    @Test
    void givenPersistedCategory_whenCallUpdate_thenSaveTheNewValues() throws Exception {
        final var category = givenPersistedCategory("Flmes", true);

        final var response = this.mockMvc.perform(put(CATEGORIES_PATH + "/" + category.getId().getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Filmes","description":"A mais assistida","active":false}"""));

        response.andExpect(status().isOk());
        final var actualCategory = this.categoryGateway.findById(category.getId()).orElseThrow();
        assertEquals("Filmes", actualCategory.getName());
    }

    @Test
    void givenPersistedCategory_whenCallDelete_thenRemoveItAndReturn204() throws Exception {
        final var category = givenPersistedCategory("Filmes", true);

        final var response = this.mockMvc.perform(delete(CATEGORIES_PATH + "/" + category.getId().getValue()));

        response.andExpect(status().isNoContent());
        assertEquals(0, this.categoryRepository.count());
    }

    private Category givenPersistedCategory(final String name, final boolean isActive) {
        return this.categoryGateway.create(Category.newCategory(name, "descricao de " + name, isActive));
    }
}
