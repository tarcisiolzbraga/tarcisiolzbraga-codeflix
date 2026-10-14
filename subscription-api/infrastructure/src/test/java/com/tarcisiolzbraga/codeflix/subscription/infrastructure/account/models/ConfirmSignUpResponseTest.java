package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models;

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
class ConfirmSignUpResponseTest {

    @Autowired
    private JacksonTester<ConfirmSignUpResponse> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenAResponse_whenSerializes_thenWritesExactlyTheFieldsOfTheContract() throws IOException {
        final var response = new ConfirmSignUpResponse("11111111-1111-1111-1111-111111111111");

        final var actual = this.json.write(response);

        assertEquals(Set.of("accountId"), fieldsOf(actual.getJson()).keySet());
    }

    @Test
    void givenAResponse_whenSerializes_thenKeepsTheValue() throws IOException {
        final var response = new ConfirmSignUpResponse("11111111-1111-1111-1111-111111111111");

        final var actual = this.json.write(response);

        assertEquals("11111111-1111-1111-1111-111111111111", fieldsOf(actual.getJson()).get("accountId"));
    }

    private Map<String, Object> fieldsOf(final String content) throws IOException {
        return this.objectMapper.readValue(content, new TypeReference<>() {});
    }
}
