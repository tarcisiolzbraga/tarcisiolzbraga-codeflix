package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

// Só leitura: estas mensagens chegam da fila, a aplicação nunca as escreve.
@JsonTest
class VideoEncoderResultTest {

    private static final String VIDEO_ID = "123";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenACompletedPayload_whenRead_thenBuildTheCompletedResult() {
        final var json =
                """
                {"status":"COMPLETED","videoId":"123","type":"VIDEO","checksum":"abc1","encodedPath":"encoded/duna.mp4"}""";

        final var actualResult = this.objectMapper.readValue(json, VideoEncoderResult.class);

        final var completed = assertInstanceOf(VideoEncoderCompleted.class, actualResult);
        assertEquals(VIDEO_ID, completed.videoId());
        assertEquals("VIDEO", completed.type());
        assertEquals("abc1", completed.checksum());
        assertEquals("encoded/duna.mp4", completed.encodedPath());
    }

    @Test
    void givenAProcessingPayload_whenRead_thenBuildTheProcessingResult() {
        final var json = """
                {"status":"PROCESSING","videoId":"123","type":"TRAILER","checksum":"abc2"}""";

        final var actualResult = this.objectMapper.readValue(json, VideoEncoderResult.class);

        final var processing = assertInstanceOf(VideoEncoderProcessing.class, actualResult);
        assertEquals("TRAILER", processing.type());
        assertEquals("abc2", processing.checksum());
    }

    @Test
    void givenAnErrorPayload_whenRead_thenBuildTheErrorResult() {
        final var json =
                """
                {"status":"ERROR","videoId":"123","type":"VIDEO","checksum":"abc3","message":"codec não suportado"}""";

        final var actualResult = this.objectMapper.readValue(json, VideoEncoderResult.class);

        final var error = assertInstanceOf(VideoEncoderError.class, actualResult);
        assertEquals("codec não suportado", error.message());
    }

    @Test
    void givenAnUnknownStatus_whenRead_thenFail() {
        final var json = """
                {"status":"CANCELLED","videoId":"123","type":"VIDEO"}""";

        assertThrows(JacksonException.class, () -> this.objectMapper.readValue(json, VideoEncoderResult.class));
    }
}
