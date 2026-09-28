package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tarcisiolzbraga.codeflix.admin.application.video.update.UpdateVideoOutput;
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
class UpdateVideoResponseTest {

    @Autowired
    private JacksonTester<UpdateVideoResponse> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenResponse_whenSerialize_thenWriteOnlyTheId() throws IOException {
        final var actualJson = this.json.write(UpdateVideoResponse.from(new UpdateVideoOutput("123")));

        final Map<String, Object> fields =
                this.objectMapper.readValue(actualJson.getJson(), new TypeReference<>() {});
        assertEquals(Set.of("id"), fields.keySet());
        assertEquals("123", fields.get("id"));
    }
}
