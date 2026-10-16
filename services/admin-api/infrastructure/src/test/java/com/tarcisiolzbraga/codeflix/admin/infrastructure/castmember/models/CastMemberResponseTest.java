package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tarcisiolzbraga.codeflix.admin.application.castmember.CastMemberOutput;
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
class CastMemberResponseTest {

    private static final Set<String> EXPECTED_FIELDS =
            Set.of("id", "name", "type", "active", "createdAt", "updatedAt");
    private static final Instant CREATED_AT = Instant.parse("2026-01-31T10:15:30.123456Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-02-01T08:00:00Z");

    @Autowired
    private JacksonTester<CastMemberResponse> json;

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
        assertEquals(false, fields.get("active"));
        assertEquals(CREATED_AT.toString(), fields.get("createdAt"));
        assertEquals(UPDATED_AT.toString(), fields.get("updatedAt"));
    }

    @Test
    void givenOutput_whenCallFrom_thenCopyEveryField() {
        final var output = new CastMemberOutput("123", "Vin Diesel", "DIRECTOR", true, CREATED_AT, UPDATED_AT);

        final var actualResponse = CastMemberResponse.from(output);

        assertEquals(new CastMemberResponse("123", "Vin Diesel", "DIRECTOR", true, CREATED_AT, UPDATED_AT),
                actualResponse);
    }

    private CastMemberResponse responseFixture() {
        return new CastMemberResponse("123", "Vin Diesel", "ACTOR", false, CREATED_AT, UPDATED_AT);
    }

    private Map<String, Object> fieldsOf(final JsonContent<CastMemberResponse> content) throws IOException {
        return this.objectMapper.readValue(content.getJson(), new TypeReference<>() {});
    }
}
