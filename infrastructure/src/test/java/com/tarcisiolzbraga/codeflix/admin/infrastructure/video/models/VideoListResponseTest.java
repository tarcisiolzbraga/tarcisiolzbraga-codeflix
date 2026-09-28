package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tarcisiolzbraga.codeflix.admin.application.video.list.VideoListOutput;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@JsonTest
class VideoListResponseTest {

    private static final Set<String> EXPECTED_FIELDS =
            Set.of("id", "title", "launchedAt", "published", "active", "createdAt");
    private static final Instant CREATED_AT = Instant.parse("2026-01-31T10:15:30.123456Z");

    @Autowired
    private JacksonTester<VideoListResponse> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenResponse_whenSerialize_thenWriteExactlyTheFieldsOfTheContract() throws IOException {
        final var actualJson = this.json.write(
                new VideoListResponse("123", "Duna", 2021, true, true, CREATED_AT));

        final Map<String, Object> fields =
                this.objectMapper.readValue(actualJson.getJson(), new TypeReference<>() {});
        assertEquals(EXPECTED_FIELDS, fields.keySet());
        assertEquals("Duna", fields.get("title"));
        assertEquals(CREATED_AT.toString(), fields.get("createdAt"));
    }

    @Test
    void givenOutput_whenCallFrom_thenCopyEveryField() {
        final var output = new VideoListOutput("123", "Duna", 2021, false, true, CREATED_AT);

        final var actualResponse = VideoListResponse.from(output);

        assertEquals(new VideoListResponse("123", "Duna", 2021, false, true, CREATED_AT), actualResponse);
    }
}
