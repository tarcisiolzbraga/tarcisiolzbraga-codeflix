package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest
class OutboxRelayIT {

    private static final long RECEIVE_TIMEOUT = 5000L;

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

    private Video videoWithMedia() {
        final var video = VideoFixture.video();
        video.updateVideoMedia(AudioVideoMedia.with("abc1", "duna.mp4", "raw/video"));
        return video;
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
