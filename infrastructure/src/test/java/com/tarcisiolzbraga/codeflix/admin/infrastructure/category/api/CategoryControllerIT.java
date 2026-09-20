package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Fumaça da fiação HTTP -> caso de uso -> banco. O comportamento de cada caso de uso
// é coberto pelos UseCaseIT, e o contrato HTTP pelo CategoryControllerTest.
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
    void givenPersistedCategory_whenCallGetById_thenReturnIt() throws Exception {
        final var category =
                this.categoryGateway.create(Category.newCategory("Filmes", "A mais assistida", true));

        final var response =
                this.mockMvc.perform(get(CATEGORIES_PATH + "/" + category.getId().getValue()));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(category.getId().getValue()))
                .andExpect(jsonPath("$.name").value("Filmes"))
                .andExpect(jsonPath("$.active").value(true));
    }
}
