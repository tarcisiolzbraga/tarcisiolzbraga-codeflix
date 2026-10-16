package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@JsonTest
class CreatePlanResponseTest {

    private static final String ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private JacksonTester<CreatePlanResponse> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenAResponse_whenSerializes_thenWritesOnlyTheId() throws IOException {
        final var response = new CreatePlanResponse(ID);

        final var actual = this.json.write(response);

        assertEquals(Set.of("id"), fieldsOf(actual.getJson()).keySet());
    }

    @Test
    void givenAResponse_whenSerializes_thenKeepsTheIdValue() throws IOException {
        final var response = new CreatePlanResponse(ID);

        final var actual = this.json.write(response);

        assertEquals(ID, fieldsOf(actual.getJson()).get("id"));
    }

    private Map<String, Object> fieldsOf(final String content) throws IOException {
        return this.objectMapper.readValue(content, new TypeReference<>() {});
    }
}
