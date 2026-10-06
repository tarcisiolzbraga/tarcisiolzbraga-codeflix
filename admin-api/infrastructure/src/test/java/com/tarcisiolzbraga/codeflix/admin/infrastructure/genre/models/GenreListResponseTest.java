package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tarcisiolzbraga.codeflix.admin.application.genre.list.GenreListOutput;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
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
class GenreListResponseTest {

    private static final Set<String> EXPECTED_FIELDS = Set.of("id", "name", "categories", "active", "createdAt");
    private static final Instant CREATED_AT = Instant.parse("2026-01-31T10:15:30.123456Z");

    @Autowired
    private JacksonTester<GenreListResponse> json;

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
        assertEquals("Ação", fields.get("name"));
        assertEquals(List.of("c1", "c2"), fields.get("categories"));
        assertEquals(true, fields.get("active"));
        assertEquals(CREATED_AT.toString(), fields.get("createdAt"));
    }

    @Test
    void givenOutputWithCategories_whenCallFrom_thenSortThem() {
        final var output = new GenreListOutput("123", "Ação", true, Set.of("c3", "c1", "c2"), CREATED_AT);

        final var actualResponse = GenreListResponse.from(output);

        assertEquals(List.of("c1", "c2", "c3"), actualResponse.categories());
    }

    private GenreListResponse responseFixture() {
        return new GenreListResponse("123", "Ação", List.of("c1", "c2"), true, CREATED_AT);
    }

    private Map<String, Object> fieldsOf(final JsonContent<GenreListResponse> content) throws IOException {
        return this.objectMapper.readValue(content.getJson(), new TypeReference<>() {});
    }
}
