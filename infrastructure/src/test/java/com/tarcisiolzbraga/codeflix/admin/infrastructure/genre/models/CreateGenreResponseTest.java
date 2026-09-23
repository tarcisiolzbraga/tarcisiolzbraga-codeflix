package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@JsonTest
class CreateGenreResponseTest {

    @Autowired
    private JacksonTester<CreateGenreResponse> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenResponse_whenSerialize_thenWriteOnlyTheId() throws IOException {
        final var response = new CreateGenreResponse("123");

        final var actualJson = this.json.write(response);

        final Map<String, Object> fields =
                this.objectMapper.readValue(actualJson.getJson(), new TypeReference<>() {});
        assertEquals(Map.of("id", "123"), fields);
    }
}
