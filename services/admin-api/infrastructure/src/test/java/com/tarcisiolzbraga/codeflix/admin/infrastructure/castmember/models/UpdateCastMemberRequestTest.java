package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class UpdateCastMemberRequestTest {

    @Autowired
    private JacksonTester<UpdateCastMemberRequest> json;

    @Test
    void givenJsonWithNameAndType_whenDeserialize_thenReadBoth() throws IOException {
        final var content = """
                {"name":"Vin Diesel","type":"DIRECTOR"}
                """;

        final var actualRequest = this.json.parseObject(content);

        assertEquals("Vin Diesel", actualRequest.name());
        assertEquals("DIRECTOR", actualRequest.type());
    }

    @Test
    void givenJsonWithoutType_whenDeserialize_thenLeaveItNull() throws IOException {
        final var content = """
                {"name":"Vin Diesel"}
                """;

        final var actualRequest = this.json.parseObject(content);

        assertNull(actualRequest.type());
    }

    @Test
    void givenJsonWithActive_whenDeserialize_thenIgnoreIt() throws IOException {
        final var content = """
                {"name":"Vin Diesel","type":"ACTOR","active":false}
                """;

        final var actualRequest = this.json.parseObject(content);

        assertEquals("Vin Diesel", actualRequest.name());
        assertEquals("ACTOR", actualRequest.type());
    }
}
