package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class UpdateCategoryRequestTest {

    @Autowired
    private JacksonTester<UpdateCategoryRequest> json;

    @Test
    void givenJsonWithNameAndDescription_whenDeserialize_thenReadBoth() throws IOException {
        final var actualRequest = this.json
                .parse("""
                        {"name":"Filmes","description":"A mais assistida"}""")
                .getObject();

        assertEquals("Filmes", actualRequest.name());
        assertEquals("A mais assistida", actualRequest.description());
    }

    @Test
    void givenJsonWithoutDescription_whenDeserialize_thenLeaveItNull() throws IOException {
        final var actualRequest = this.json
                .parse("""
                        {"name":"Filmes"}""")
                .getObject();

        assertEquals("Filmes", actualRequest.name());
        assertNull(actualRequest.description());
    }

    // A ativação saiu do PUT e passou a ter rotas próprias; corpo antigo com active não volta a valer.
    @Test
    void givenJsonWithActive_whenDeserialize_thenIgnoreIt() throws IOException {
        final var actualRequest = this.json
                .parse("""
                        {"name":"Filmes","description":"A mais assistida","active":false}""")
                .getObject();

        assertEquals("Filmes", actualRequest.name());
    }
}
