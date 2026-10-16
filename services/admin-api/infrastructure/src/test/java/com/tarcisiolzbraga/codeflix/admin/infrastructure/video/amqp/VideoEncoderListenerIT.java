package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.amqp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tarcisiolzbraga.codeflix.admin.domain.video.AudioVideoMedia;
import com.tarcisiolzbraga.codeflix.admin.domain.video.MediaStatus;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoFixture;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.application.video.media.update.UpdateMediaStatusUseCase;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration.AmqpProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

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

    // Espião, e não dublê: o comportamento real é o que faz a NotFoundException subir. O que se
    // quer dele é só a contagem de chamadas.
    @MockitoSpyBean
    private UpdateMediaStatusUseCase updateMediaStatusUseCase;

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

    // A resposta se lê perfeitamente, mas fala de um vídeo que não está aqui — apagado entre o
    // envio e a resposta, ou vindo de outro ambiente. A NotFoundException subia do caso de uso e o
    // Spring devolvia a mensagem à fila, que voltava e falhava sem parar.
    //
    // O que denuncia o laço é a CONTAGEM, não o resultado: mesmo em laço o consumidor processa as
    // outras mensagens no meio, então "continua funcionando" passa dos dois jeitos. Uma única
    // chamada ao caso de uso é o que prova que a mensagem foi descartada em vez de recircular.
    @Test
    void givenAnAnswerForAVideoThatIsNotHere_whenTheEncoderSendsIt_thenProcessItOnceAndDropIt() {
        final var fantasma = VideoID.unique().getValue();

        send("""
                {"status":"COMPLETED","videoId":"%s","type":"VIDEO","checksum":"abc1",\
"encodedPath":"encoded/fantasma.mp4"}"""
                .formatted(fantasma));

        awaitAtLeastOneCall();
        sleepFor(Duration.ofSeconds(2));
        verify(this.updateMediaStatusUseCase, times(1)).execute(argThat(command -> fantasma.equals(command.videoId())));
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

    private void awaitAtLeastOneCall() {
        final var deadline = Instant.now().plus(TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            if (!mockingDetails(this.updateMediaStatusUseCase).getInvocations().isEmpty()) {
                return;
            }
            sleep();
        }
        throw new AssertionError("o caso de uso nunca foi chamado");
    }

    private void sleepFor(final Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
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
