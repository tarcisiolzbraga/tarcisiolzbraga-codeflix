package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoImageOutput;
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
class ImageMediaResponseTest {

    private static final Set<String> EXPECTED_FIELDS = Set.of("checksum", "name", "location");

    @Autowired
    private JacksonTester<ImageMediaResponse> json;

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
        assertEquals("abc2", fields.get("checksum"));
        assertEquals("duna.png", fields.get("name"));
        assertEquals("raw/banner", fields.get("location"));
    }

    @Test
    void givenNoOutput_whenCallFrom_thenReceiveNull() {
        assertNull(ImageMediaResponse.from(null));
    }

    private ImageMediaResponse responseFixture() {
        return ImageMediaResponse.from(new VideoImageOutput("abc2", "duna.png", "raw/banner"));
    }
}
