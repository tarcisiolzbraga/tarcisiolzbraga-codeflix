package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class CreateGenreRequestTest {

    @Autowired
    private JacksonTester<CreateGenreRequest> json;

    @Test
    void givenJsonWithEveryField_whenDeserialize_thenReadThemAll() throws IOException {
        final var actualRequest = this.json
                .parse("""
                        {"name":"Ação","categories":["c1","c2"],"active":false}""")
                .getObject();

        assertEquals("Ação", actualRequest.name());
        assertEquals(Set.of("c1", "c2"), actualRequest.categories());
        assertFalse(actualRequest.isActive());
    }

    @Test
    void givenJsonWithOnlyName_whenDeserialize_thenTheGenreIsActiveWithoutCategories() throws IOException {
        final var actualRequest = this.json
                .parse("""
                        {"name":"Ação"}""")
                .getObject();

        assertNull(actualRequest.active());
        assertTrue(actualRequest.isActive());
        assertNull(actualRequest.categories());
    }

    @Test
    void givenJsonWithRepeatedCategory_whenDeserialize_thenKeepItOnce() throws IOException {
        final var actualRequest = this.json
                .parse("""
                        {"name":"Ação","categories":["c1","c1"]}""")
                .getObject();

        assertEquals(Set.of("c1"), actualRequest.categories());
    }

    @Test
    void givenJsonWithUnknownField_whenDeserialize_thenIgnoreIt() throws IOException {
        final var actualRequest = this.json
                .parse("""
                        {"name":"Ação","deletedAt":"2026-01-01T00:00:00Z"}""")
                .getObject();

        assertEquals("Ação", actualRequest.name());
        assertTrue(actualRequest.isActive());
    }
}
