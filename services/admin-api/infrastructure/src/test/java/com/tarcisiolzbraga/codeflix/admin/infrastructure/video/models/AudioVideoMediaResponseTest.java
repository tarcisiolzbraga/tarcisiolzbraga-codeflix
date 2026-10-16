package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoMediaOutput;
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
class AudioVideoMediaResponseTest {

    private static final Set<String> EXPECTED_FIELDS =
            Set.of("checksum", "name", "rawLocation", "encodedLocation", "status");

    @Autowired
    private JacksonTester<AudioVideoMediaResponse> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenResponse_whenSerialize_thenWriteExactlyTheFieldsOfTheContract() throws IOException {
        final var actualJson = this.json.write(responseFixture());

        final Map<String, Object> fields =
                this.objectMapper.readValue(actualJson.getJson(), new TypeReference<>() {});
        assertEquals(EXPECTED_FIELDS, fields.keySet());
    }

    @Test
    void givenResponse_whenSerialize_thenKeepTheValues() throws IOException {
        final var actualJson = this.json.write(responseFixture());

        final Map<String, Object> fields =
                this.objectMapper.readValue(actualJson.getJson(), new TypeReference<>() {});
        assertEquals("abc1", fields.get("checksum"));
        assertEquals("duna.mp4", fields.get("name"));
        assertEquals("raw/video", fields.get("rawLocation"));
        assertEquals("", fields.get("encodedLocation"));
        assertEquals("PENDING", fields.get("status"));
    }

    @Test
    void givenNoOutput_whenCallFrom_thenReceiveNull() {
        assertNull(AudioVideoMediaResponse.from(null));
    }

    private AudioVideoMediaResponse responseFixture() {
        return AudioVideoMediaResponse.from(
                new VideoMediaOutput("abc1", "duna.mp4", "raw/video", "", "PENDING"));
    }
}
