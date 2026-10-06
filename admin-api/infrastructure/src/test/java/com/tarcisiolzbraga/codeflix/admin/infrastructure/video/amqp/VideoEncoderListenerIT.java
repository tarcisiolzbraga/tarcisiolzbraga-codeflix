package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.amqp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tarcisiolzbraga.codeflix.admin.domain.video.AudioVideoMedia;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaStatus;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoFixture;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration.AmqpProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

// A jornada da fila: a mensagem entra pelo broker de verdade e o vídeo muda no banco de verdade.
@IntegrationTest
class VideoEncoderListenerIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final long POLL_INTERVAL = 100L;

    @Autowired
    private VideoGateway videoGateway;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpProperties amqpProperties;

    @Test
    void givenACompletedMessage_whenTheEncoderSendsIt_thenTheVideoKeepsTheEncodedPath() {
        final var video = givenStoredVideoWithMedia();

        send("""
                {"status":"COMPLETED","videoId":"%s","type":"VIDEO","checksum":"abc1","encodedPath":"encoded/duna.mp4"}"""
                .formatted(video.getId().getValue()));

        final var actualMedia = awaitMedia(video.getId(), media -> media.status() == MediaStatus.COMPLETED);
        assertEquals("encoded/duna.mp4", actualMedia.encodedLocation());
    }

    @Test
    void givenAProcessingMessage_whenTheEncoderSendsIt_thenTheVideoMoves() {
        final var video = givenStoredVideoWithMedia();

        send("""
                {"status":"PROCESSING","videoId":"%s","type":"VIDEO","checksum":"abc1"}""".formatted(video.getId().getValue()));

        final var actualMedia = awaitMedia(video.getId(), media -> media.status() == MediaStatus.PROCESSING);
        assertEquals("", actualMedia.encodedLocation());
    }

    @Test
    void givenAnUnreadableMessage_whenTheEncoderSendsIt_thenDropItAndKeepWorking() {
        final var video = givenStoredVideoWithMedia();

        send("isto não é json");
        send("""
                {"status":"COMPLETED","videoId":"%s","type":"VIDEO","checksum":"abc1","encodedPath":"encoded/duna.mp4"}"""
                .formatted(video.getId().getValue()));

        final var actualMedia = awaitMedia(video.getId(), media -> media.status() == MediaStatus.COMPLETED);
        assertEquals("encoded/duna.mp4", actualMedia.encodedLocation());
    }

    @Test
    void givenAnAnswerForAReplacedFile_whenTheEncoderSendsIt_thenIgnoreIt() {
        final var video = givenStoredVideoWithMedia();

        send("""
                {"status":"COMPLETED","videoId":"%s","type":"VIDEO","checksum":"envio-antigo",\
"encodedPath":"encoded/antigo.mp4"}"""
                .formatted(video.getId().getValue()));
        send("""
                {"status":"PROCESSING","videoId":"%s","type":"VIDEO","checksum":"abc1"}"""
                .formatted(video.getId().getValue()));

        final var actualMedia = awaitMedia(video.getId(), media -> media.status() == MediaStatus.PROCESSING);
        assertEquals("", actualMedia.encodedLocation());
    }

    private void send(final String payload) {
        this.rabbitTemplate.convertAndSend(
                this.amqpProperties.exchange(),
                this.amqpProperties.queues().videoEncoded().routingKey(),
                payload);
    }

    private Video givenStoredVideoWithMedia() {
        final var video = VideoFixture.video();
        video.updateVideoMedia(AudioVideoMedia.with("abc1", "duna.mp4", "raw/video"));
        return this.videoGateway.create(video);
    }

    // O consumidor é assíncrono: esperar pela condição é mais honesto que um sleep fixo.
    private AudioVideoMedia awaitMedia(final VideoID id, final Predicate<AudioVideoMedia> condition) {
        final var deadline = Instant.now().plus(TIMEOUT);
        AudioVideoMedia media = null;
        while (Instant.now().isBefore(deadline)) {
            media = this.videoGateway.findById(id).flatMap(Video::getVideo).orElse(null);
            if (media != null && condition.test(media)) {
                return media;
            }
            sleep();
        }
        throw new AssertionError("a mídia não chegou ao estado esperado: " + media);
    }

    private void sleep() {
        try {
            Thread.sleep(POLL_INTERVAL);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
