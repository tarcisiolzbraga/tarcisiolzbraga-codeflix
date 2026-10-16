package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class UpdateGenreRequestTest {

    @Autowired
    private JacksonTester<UpdateGenreRequest> json;

    @Test
    void givenJsonWithNameAndCategories_whenDeserialize_thenReadBoth() throws IOException {
        final var actualRequest = this.json
                .parse("""
                        {"name":"Ação","categories":["c1","c2"]}""")
                .getObject();

        assertEquals("Ação", actualRequest.name());
        assertEquals(Set.of("c1", "c2"), actualRequest.categories());
    }

    @Test
    void givenJsonWithoutCategories_whenDeserialize_thenLeaveThemNull() throws IOException {
        final var actualRequest = this.json
                .parse("""
                        {"name":"Ação"}""")
                .getObject();

        assertEquals("Ação", actualRequest.name());
        assertNull(actualRequest.categories());
    }

    // A ativação tem rotas próprias; active no corpo do PUT não vale.
    @Test
    void givenJsonWithActive_whenDeserialize_thenIgnoreIt() throws IOException {
        final var actualRequest = this.json
                .parse("""
                        {"name":"Ação","categories":[],"active":false}""")
                .getObject();

        assertEquals("Ação", actualRequest.name());
        assertEquals(Set.of(), actualRequest.categories());
    }
}
