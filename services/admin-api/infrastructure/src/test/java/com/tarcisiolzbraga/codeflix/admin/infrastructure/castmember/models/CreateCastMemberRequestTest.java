package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models;

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
class CreateCastMemberRequestTest {

    @Autowired
    private JacksonTester<CreateCastMemberRequest> json;

    @Test
    void givenJsonWithEveryField_whenDeserialize_thenReadThemAll() throws IOException {
        final var content = """
                {"name":"Vin Diesel","type":"ACTOR","active":false}
                """;

        final var actualRequest = this.json.parseObject(content);

        assertEquals("Vin Diesel", actualRequest.name());
        assertEquals("ACTOR", actualRequest.type());
        assertFalse(actualRequest.isActive());
    }

    @Test
    void givenJsonWithoutActive_whenDeserialize_thenTheCastMemberIsActive() throws IOException {
        final var content = """
                {"name":"Vin Diesel","type":"ACTOR"}
                """;

        final var actualRequest = this.json.parseObject(content);

        assertTrue(actualRequest.isActive());
    }

    @Test
    void givenJsonWithUnknownType_whenDeserialize_thenKeepTheRawText() throws IOException {
        final var content = """
                {"name":"Vin Diesel","type":"SINGER"}
                """;

        final var actualRequest = this.json.parseObject(content);

        assertEquals("SINGER", actualRequest.type());
    }

    @Test
    void givenJsonWithUnknownField_whenDeserialize_thenIgnoreIt() throws IOException {
        final var content = """
                {"name":"Vin Diesel","type":"ACTOR","nickname":"Vin"}
                """;

        final var actualRequest = this.json.parseObject(content);

        assertEquals("Vin Diesel", actualRequest.name());
        assertNull(actualRequest.active());
    }
}
