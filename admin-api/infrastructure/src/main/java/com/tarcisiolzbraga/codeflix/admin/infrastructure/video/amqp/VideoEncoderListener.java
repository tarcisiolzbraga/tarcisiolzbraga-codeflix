package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.amqp;

import com.tarcisiolzbraga.codeflix.admin.application.video.media.update.UpdateMediaStatusCommand;
import com.tarcisiolzbraga.codeflix.admin.application.video.media.update.UpdateMediaStatusUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaStatus;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoMediaType;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.VideoEncoderCompleted;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.VideoEncoderError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.VideoEncoderProcessing;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.VideoEncoderResult;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

// Ouve o retorno do codificador. Mensagem que não dá para entender é registrada e descartada, nunca
// devolvida à fila: sem isso ela voltaria para sempre, e o consumidor não sairia do lugar.
@Component
public class VideoEncoderListener {

    static final String LISTENER_ID = "videoEncodedListener";

    private static final Logger log = LoggerFactory.getLogger(VideoEncoderListener.class);
    private static final String UNREADABLE_MESSAGE = "[message:video.encoded] [status:unreadable] [payload:{}]";
    private static final String UNKNOWN_TYPE_MESSAGE = "[message:video.encoded] [status:unknownType] [payload:{}]";
    private static final String ENCODER_ERROR_MESSAGE = "[message:video.encoded] [status:error] [payload:{}]";

    private final UpdateMediaStatusUseCase updateMediaStatusUseCase;
    private final ObjectMapper objectMapper;

    public VideoEncoderListener(
            final UpdateMediaStatusUseCase updateMediaStatusUseCase, final ObjectMapper objectMapper) {
        this.updateMediaStatusUseCase =
                Objects.requireNonNull(updateMediaStatusUseCase, "'updateMediaStatusUseCase' should not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "'objectMapper' should not be null");
    }

    @RabbitListener(id = LISTENER_ID, queues = "${amqp.queues.video-encoded.queue}")
    public void onVideoEncoded(@Payload final String message) {
        read(message).ifPresent(result -> handle(result, message));
    }

    private Optional<VideoEncoderResult> read(final String message) {
        try {
            return Optional.of(this.objectMapper.readValue(message, VideoEncoderResult.class));
        } catch (final JacksonException exception) {
            log.error(UNREADABLE_MESSAGE, message, exception);
            return Optional.empty();
        }
    }

    private void handle(final VideoEncoderResult result, final String message) {
        final var type = VideoMediaType.of(result.type());
        if (type.isEmpty()) {
            log.error(UNKNOWN_TYPE_MESSAGE, message);
            return;
        }
        switch (result) {
            case VideoEncoderCompleted completed -> execute(completed, type.get(), MediaStatus.COMPLETED);
            case VideoEncoderProcessing processing -> execute(processing, type.get(), MediaStatus.PROCESSING);
            case VideoEncoderError _ -> log.error(ENCODER_ERROR_MESSAGE, message);
        }
    }

    private void execute(final VideoEncoderResult result, final VideoMediaType type, final MediaStatus status) {
        final var encodedPath = result instanceof VideoEncoderCompleted completed ? completed.encodedPath() : "";
        this.updateMediaStatusUseCase.execute(
                new UpdateMediaStatusCommand(result.videoId(), type, status, result.checksum(), encodedPath));
    }
}
