package com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@JsonTest
class CategoryListResponseTest {

    private static final Set<String> EXPECTED_FIELDS = Set.of("id", "name", "description", "active", "createdAt");
    private static final Instant CREATED_AT = Instant.parse("2026-01-31T10:15:30.123456Z");

    @Autowired
    private JacksonTester<CategoryListResponse> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenResponse_whenSerialize_thenWriteExactlyTheFieldsOfTheContract() throws IOException {
        final var response = responseFixture();

        final var actualJson = this.json.write(response);

        assertEquals(EXPECTED_FIELDS, fieldsOf(actualJson).keySet());
    }

    @Test
    void givenResponse_whenSerialize_thenKeepTheValuesWithIsoDates() throws IOException {
        final var response = responseFixture();

        final var actualJson = this.json.write(response);

        final var fields = fieldsOf(actualJson);
        assertEquals("123", fields.get("id"));
        assertEquals("Filmes", fields.get("name"));
        assertEquals("A mais assistida", fields.get("description"));
        assertEquals(true, fields.get("active"));
        assertEquals(CREATED_AT.toString(), fields.get("createdAt"));
    }

    private CategoryListResponse responseFixture() {
        return new CategoryListResponse("123", "Filmes", "A mais assistida", true, CREATED_AT);
    }

    private Map<String, Object> fieldsOf(final JsonContent<CategoryListResponse> content) throws IOException {
        return this.objectMapper.readValue(content.getJson(), new TypeReference<>() {});
    }
}
