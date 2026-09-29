package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoFields;
import com.tarcisiolzbraga.codeflix.admin.application.video.VideoOutput;
import com.tarcisiolzbraga.codeflix.admin.application.video.VideoMediaOutputs;
import com.tarcisiolzbraga.codeflix.admin.application.video.VideoReferenceIds;
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
class VideoResponseTest {

    private static final Set<String> EXPECTED_FIELDS = Set.of(
            "id", "title", "description", "launchedAt", "duration", "rating", "opened", "published", "active",
            "categories", "genres", "castMembers", "createdAt", "updatedAt");
    private static final Instant CREATED_AT = Instant.parse("2026-01-31T10:15:30.123456Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-02-01T08:00:00Z");

    @Autowired
    private JacksonTester<VideoResponse> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenResponse_whenSerialize_thenWriteExactlyTheFieldsOfTheContract() throws IOException {
        final var actualJson = this.json.write(responseFixture());

        assertEquals(EXPECTED_FIELDS, fieldsOf(actualJson).keySet());
    }

    @Test
    void givenResponse_whenSerialize_thenKeepTheValuesWithIsoDates() throws IOException {
        final var actualJson = this.json.write(responseFixture());

        final var fields = fieldsOf(actualJson);
        assertEquals("123", fields.get("id"));
        assertEquals("Duna", fields.get("title"));
        assertEquals(2021, fields.get("launchedAt"));
        assertEquals(155.0, fields.get("duration"));
        assertEquals("12", fields.get("rating"));
        assertEquals(false, fields.get("opened"));
        assertEquals(true, fields.get("published"));
        assertEquals(List.of("c1", "c2"), fields.get("categories"));
        assertEquals(CREATED_AT.toString(), fields.get("createdAt"));
        assertEquals(UPDATED_AT.toString(), fields.get("updatedAt"));
    }

    @Test
    void givenOutputWithReferences_whenCallFrom_thenSortThem() {
        final var output = new VideoOutput(
                "123",
                new VideoFields("Duna", "Arrakis", 2021, 155.0, "12"),
                new VideoReferenceIds(Set.of("c3", "c1", "c2"), Set.of("g2", "g1"), Set.of("m1")),
                noMedias(),
                false,
                true,
                true,
                CREATED_AT,
                UPDATED_AT);

        final var actualResponse = VideoResponse.from(output);

        assertEquals(List.of("c1", "c2", "c3"), actualResponse.categories());
        assertEquals(List.of("g1", "g2"), actualResponse.genres());
    }

    private VideoResponse responseFixture() {
        return new VideoResponse(
                "123", "Duna", "Arrakis", 2021, 155.0, "12", false, true, true,
                List.of("c1", "c2"), List.of("g1"), List.of("m1"), CREATED_AT, UPDATED_AT);
    }

    private Map<String, Object> fieldsOf(final JsonContent<VideoResponse> content) throws IOException {
        return this.objectMapper.readValue(content.getJson(), new TypeReference<>() {});
    }

    private VideoMediaOutputs noMedias() {
        return new VideoMediaOutputs(null, null, null, null, null);
    }

}
