package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Cada perfil é um contexto (e um container) à parte: só o que muda entre eles é testado aqui.
class ApplicationProfileIT {

    private static final String API_DOCS_PATH = "/v3/api-docs";
    private static final String SWAGGER_UI_PATH = "/swagger-ui/index.html";

    @Nested
    @IntegrationTest
    class Development {

        @Autowired
        private Environment environment;

        @Test
        void givenNoActiveProfile_whenStartApp_thenUseDevelopment() {
            final var profileMatches = this.environment.matchesProfiles("development");

            assertTrue(profileMatches);
        }
    }

    @Nested
    @IntegrationTest
    @AutoConfigureMockMvc
    @ActiveProfiles("homolog")
    class Homolog {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void givenHomologProfile_whenCallApiDocs_thenServeTheDocs() throws Exception {
            final var response = this.mockMvc.perform(get(API_DOCS_PATH));

            response.andExpect(status().isOk());
        }
    }

    @Nested
    @IntegrationTest
    @AutoConfigureMockMvc
    @ActiveProfiles("production")
    class Production {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void givenProductionProfile_whenCallApiDocs_thenNotFound() throws Exception {
            final var response = this.mockMvc.perform(get(API_DOCS_PATH));

            response.andExpect(status().isNotFound());
        }

        @Test
        void givenProductionProfile_whenOpenSwaggerUi_thenNotFound() throws Exception {
            final var response = this.mockMvc.perform(get(SWAGGER_UI_PATH));

            response.andExpect(status().isNotFound());
        }
    }
}
