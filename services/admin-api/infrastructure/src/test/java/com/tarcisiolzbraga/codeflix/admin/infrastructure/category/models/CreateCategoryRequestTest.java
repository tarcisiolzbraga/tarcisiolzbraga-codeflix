package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class CreateCategoryRequestTest {

    @Autowired
    private JacksonTester<CreateCategoryRequest> json;

    @Test
    void givenJsonWithoutActive_whenDeserialize_thenTheCategoryIsActive() throws IOException {
        final var actualRequest = this.json
                .parse("""
                        {"name":"Filmes","description":"A mais assistida"}""")
                .getObject();

        assertNull(actualRequest.active());
        assertTrue(actualRequest.isActive());
    }

    @Test
    void givenJsonWithActiveFalse_whenDeserialize_thenTheCategoryIsInactive() throws IOException {
        final var actualRequest = this.json
                .parse("""
                        {"name":"Filmes","description":"A mais assistida","active":false}""")
                .getObject();

        assertFalse(actualRequest.isActive());
    }

    @Test
    void givenJsonWithActiveTrue_whenDeserialize_thenReadAllTheFields() throws IOException {
        final var actualRequest = this.json
                .parse("""
                        {"name":"Filmes","description":"A mais assistida","active":true}""")
                .getObject();

        assertEquals("Filmes", actualRequest.name());
        assertEquals("A mais assistida", actualRequest.description());
        assertTrue(actualRequest.isActive());
    }

    @Test
    void givenJsonWithUnknownField_whenDeserialize_thenIgnoreIt() throws IOException {
        final var actualRequest = this.json
                .parse("""
                        {"name":"Filmes","deletedAt":"2026-01-01T00:00:00Z"}""")
                .getObject();

        assertEquals("Filmes", actualRequest.name());
        assertTrue(actualRequest.isActive());
    }
}
