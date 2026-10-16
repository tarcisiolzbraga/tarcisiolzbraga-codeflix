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
class CategoryResponseTest {

    private static final Set<String> EXPECTED_FIELDS =
            Set.of("id", "name", "description", "active", "createdAt", "updatedAt");
    private static final Instant CREATED_AT = Instant.parse("2026-01-31T10:15:30.123456Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-02-01T08:00:00Z");

    @Autowired
    private JacksonTester<CategoryResponse> json;

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
        assertEquals(false, fields.get("active"));
        assertEquals(CREATED_AT.toString(), fields.get("createdAt"));
        assertEquals(UPDATED_AT.toString(), fields.get("updatedAt"));
    }

    @Test
    void givenNullDescription_whenSerialize_thenKeepTheFieldAsNull() throws IOException {
        final var response = new CategoryResponse("123", "Filmes", null, true, CREATED_AT, UPDATED_AT);

        final var actualJson = this.json.write(response);

        final var fields = fieldsOf(actualJson);
        assertEquals(EXPECTED_FIELDS, fields.keySet());
        assertEquals(null, fields.get("description"));
    }

    private CategoryResponse responseFixture() {
        return new CategoryResponse("123", "Filmes", "A mais assistida", false, CREATED_AT, UPDATED_AT);
    }

    private Map<String, Object> fieldsOf(final JsonContent<CategoryResponse> content) throws IOException {
        return this.objectMapper.readValue(content.getJson(), new TypeReference<>() {});
    }
}
