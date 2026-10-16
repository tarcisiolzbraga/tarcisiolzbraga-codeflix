package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tarcisiolzbraga.codeflix.admin.application.castmember.list.CastMemberListOutput;
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
class CastMemberListResponseTest {

    private static final Set<String> EXPECTED_FIELDS = Set.of("id", "name", "type", "active", "createdAt");
    private static final Instant CREATED_AT = Instant.parse("2026-01-31T10:15:30.123456Z");

    @Autowired
    private JacksonTester<CastMemberListResponse> json;

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
        assertEquals("Vin Diesel", fields.get("name"));
        assertEquals("ACTOR", fields.get("type"));
        assertEquals(true, fields.get("active"));
        assertEquals(CREATED_AT.toString(), fields.get("createdAt"));
    }

    @Test
    void givenOutput_whenCallFrom_thenCopyEveryField() {
        final var output = new CastMemberListOutput("123", "Vin Diesel", "DIRECTOR", false, CREATED_AT);

        final var actualResponse = CastMemberListResponse.from(output);

        assertEquals(new CastMemberListResponse("123", "Vin Diesel", "DIRECTOR", false, CREATED_AT), actualResponse);
    }

    private CastMemberListResponse responseFixture() {
        return new CastMemberListResponse("123", "Vin Diesel", "ACTOR", true, CREATED_AT);
    }

    private Map<String, Object> fieldsOf(final JsonContent<CastMemberListResponse> content) throws IOException {
        return this.objectMapper.readValue(content.getJson(), new TypeReference<>() {});
    }
}
