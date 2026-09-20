package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class OpenApiIT {

    private static final String API_DOCS_PATH = "/v3/api-docs";
    private static final String CATEGORIES = "$.paths['/categories']";
    private static final String CATEGORY_BY_ID = "$.paths['/categories/{id}']";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void givenRunningApp_whenCallApiDocs_thenDescribeTheApi() throws Exception {
        final var response = this.mockMvc.perform(get(API_DOCS_PATH));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Codeflix Admin"))
                .andExpect(jsonPath("$.info.version").value("v1"));
    }

    @Test
    void givenCategoryApi_whenCallApiDocs_thenDocumentTheFiveOperations() throws Exception {
        final var response = this.mockMvc.perform(get(API_DOCS_PATH));

        response.andExpect(jsonPath(CATEGORIES + ".post").exists())
                .andExpect(jsonPath(CATEGORIES + ".get").exists())
                .andExpect(jsonPath(CATEGORY_BY_ID + ".get").exists())
                .andExpect(jsonPath(CATEGORY_BY_ID + ".put").exists())
                .andExpect(jsonPath(CATEGORY_BY_ID + ".delete").exists());
    }

    @Test
    void givenSearchRequest_whenCallApiDocs_thenExpandItIntoQueryParams() throws Exception {
        final var response = this.mockMvc.perform(get(API_DOCS_PATH));

        response.andExpect(jsonPath(CATEGORIES + ".get.parameters[*].name")
                .value(hasItems("search", "page", "perPage", "sort", "dir")));
    }

    @Test
    void givenErrorResponses_whenCallApiDocs_thenDocumentThemOnlyWhereTheyHappen() throws Exception {
        final var response = this.mockMvc.perform(get(API_DOCS_PATH));

        response.andExpect(jsonPath(CATEGORY_BY_ID + ".get.responses['404']").exists())
                .andExpect(jsonPath(CATEGORIES + ".post.responses['422']").exists())
                .andExpect(jsonPath(CATEGORIES + ".post.responses['404']").doesNotExist())
                .andExpect(jsonPath(CATEGORY_BY_ID + ".delete.responses['404']").doesNotExist());
    }

    @Test
    void givenRunningApp_whenOpenSwaggerUi_thenServeThePage() throws Exception {
        final var response = this.mockMvc.perform(get("/swagger-ui/index.html"));

        response.andExpect(status().isOk());
    }
}
