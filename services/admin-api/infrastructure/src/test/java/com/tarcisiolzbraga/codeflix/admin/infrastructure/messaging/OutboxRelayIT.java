package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import com.tarcisiolzbraga.codeflix.admin.domain.video.AudioVideoMedia;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoFixture;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration.AmqpProperties;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.persistence.OutboxEventRepository;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest
class OutboxRelayIT {

    private static final long RECEIVE_TIMEOUT = 5000L;
    private static final long LATCH_TIMEOUT = 10L;
    private static final int ROWS_IN_RACE = 6;

    @Autowired
    private OutboxRelay relay;

    @Autowired
    private VideoGateway videoGateway;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpProperties amqpProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private RabbitEventSender sender;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void givenAPendingRow_whenDeliver_thenTheMessageReachesTheQueue() {
        final var video = this.videoGateway.create(videoWithMedia());

        this.relay.deliverPending();

        final var message = receive();
        assertNotNull(message);
        assertEquals(video.getId().getValue(), message.get("videoId"));
        assertEquals("abc1", message.get("checksum"));
    }

    @Test
    void givenADeliveredRow_whenDeliver_thenMarkItSent() {
        this.videoGateway.create(videoWithMedia());

        this.relay.deliverPending();

        assertNotNull(this.outboxRepository.findAll().getFirst().getSentAt());
    }

    @Test
    void givenAnAlreadyDeliveredRow_whenDeliverAgain_thenSendNothing() {
        this.videoGateway.create(videoWithMedia());
        this.relay.deliverPending();
        drain();

        this.relay.deliverPending();

        assertNull(this.rabbitTemplate.receiveAndConvert(queue(), 500L));
    }

    @Test
    void givenABrokerOutage_whenDeliver_thenLeaveTheRowPendingForTheNextTry() {
        this.videoGateway.create(videoWithMedia());
        doThrow(new IllegalStateException("broker indisponível")).when(this.sender).send(any(), any());

        assertThrows(IllegalStateException.class, () -> this.relay.deliverPending());

        assertNull(this.outboxRepository.findAll().getFirst().getSentAt());
    }

    @Test
    void givenTheSecondSendFailing_whenDeliver_thenKeepTheFirstOneDelivered() {
        this.videoGateway.create(videoWithMedia());
        this.videoGateway.create(videoWithMedia());
        doNothing()
                .doThrow(new IllegalStateException("broker indisponível"))
                .when(this.sender)
                .send(any(), any());

        assertThrows(IllegalStateException.class, () -> this.relay.deliverPending());

        final var rows = this.outboxRepository.findBySentAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, 10));
        assertEquals(1, rows.getTotalElements());
        assertEquals(2, this.outboxRepository.count());
    }


    // O teste do SKIP LOCKED, que é o que permite mais de uma instância do relay.
    //
    // Uma transação em outra thread trava a linha mais antiga e a segura. O relay tem de entregar a
    // **outra** — nem bloquear esperando o lock, nem reler a travada e entregar em dobro. Sem o
    // hint de skip-locked este teste estoura no tempo em vez de falhar na asserção, e é essa a
    // diferença entre as duas configurações erradas possíveis.
    @Test
    void givenTheOldestRowLockedByAnotherTransaction_whenDeliver_thenSkipItAndDeliverTheNextOne() throws Exception {
        this.videoGateway.create(videoWithMedia("primeira"));
        this.videoGateway.create(videoWithMedia("segunda"));
        final var locked = new CountDownLatch(1);
        final var release = new CountDownLatch(1);
        final var holder = Executors.newSingleThreadExecutor();
        holder.submit(() -> this.transactions.execute(status -> {
            this.outboxRepository.lockNextPending();
            locked.countDown();
            awaitQuietly(release);
            return null;
        }));
        assertTrue(locked.await(LATCH_TIMEOUT, TimeUnit.SECONDS), "a outra transação não travou a linha");

        this.relay.deliverPending();

        release.countDown();
        holder.shutdown();
        assertEquals("segunda", receive().get("checksum"));
        assertEquals(1, this.outboxRepository.findBySentAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, 10))
                .getTotalElements());
    }

    // O outro lado da mesma propriedade: duas passagens ao mesmo tempo sobre a mesma fila entregam
    // cada linha **uma vez**, e não duas. É o que a instância única garantia de graça e que o lock
    // passa a garantir.
    @Test
    void givenTwoRelayPassesAtOnce_whenTheyRace_thenEachRowIsDeliveredExactlyOnce() throws Exception {
        for (var i = 0; i < ROWS_IN_RACE; i++) {
            this.videoGateway.create(videoWithMedia("c" + i));
        }
        final var start = new CountDownLatch(1);
        final var pool = Executors.newFixedThreadPool(2);

        for (var i = 0; i < 2; i++) {
            pool.submit(() -> {
                awaitQuietly(start);
                this.relay.deliverPending();
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(LATCH_TIMEOUT, TimeUnit.SECONDS), "as passagens não terminaram");

        assertEquals(ROWS_IN_RACE, countReceived());
        assertEquals(0, this.outboxRepository.findBySentAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, 50))
                .getTotalElements());
    }

    private Video videoWithMedia() {
        return videoWithMedia("abc1");
    }

    private Video videoWithMedia(final String checksum) {
        final var video = VideoFixture.video();
        video.updateVideoMedia(AudioVideoMedia.with(checksum, "duna.mp4", "raw/video"));
        return video;
    }

    private static void awaitQuietly(final CountDownLatch latch) {
        try {
            latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private int countReceived() {
        var received = 0;
        while (this.rabbitTemplate.receiveAndConvert(queue(), 500L) != null) {
            received++;
        }
        return received;
    }

    private String queue() {
        return this.amqpProperties.queues().videoCreated().queue();
    }

    private Map<String, Object> receive() {
        final var payload = this.rabbitTemplate.receiveAndConvert(queue(), RECEIVE_TIMEOUT);
        return payload == null ? null : this.objectMapper.readValue(payload.toString(), new TypeReference<>() {});
    }

    private void drain() {
        this.rabbitTemplate.receiveAndConvert(queue(), RECEIVE_TIMEOUT);
    }
}
