package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest
class OutboxDomainEventPublisherIT {

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

    @Test
    void givenAVideoWithANewMedia_whenSave_thenLeaveOnePendingRow() {
        final var video = videoWithMedia();

        this.videoGateway.create(video);

        final var rows = this.outboxRepository.findAll();
        assertEquals(1, rows.size());
        final var row = rows.getFirst();
        assertEquals("video.created", row.getRoutingKey());
        assertNotNull(row.getCreatedAt());
        assertNull(row.getSentAt());
    }

    @Test
    void givenAPendingRow_whenReadItsPayload_thenCarryTheEventAsItWas() {
        final var video = this.videoGateway.create(videoWithMedia());

        final Map<String, Object> payload = this.objectMapper.readValue(
                this.outboxRepository.findAll().getFirst().getPayload(), new TypeReference<>() {});

        assertEquals(video.getId().getValue(), payload.get("videoId"));
        assertEquals("VIDEO", payload.get("type"));
        assertEquals("raw/video", payload.get("filePath"));
        assertEquals("abc1", payload.get("checksum"));
    }

    @Test
    void givenAVideoWithANewMedia_whenSave_thenSendNothingToTheBrokerYet() {
        this.videoGateway.create(videoWithMedia());

        assertNull(this.rabbitTemplate.receiveAndConvert(
                this.amqpProperties.queues().videoCreated().queue(), 500L));
    }

    @Test
    void givenAnAlreadyAnnouncedVideo_whenSaveAgain_thenLeaveNoNewRow() {
        final var video = this.videoGateway.create(videoWithMedia());

        this.videoGateway.update(video);

        assertEquals(1, this.outboxRepository.count());
    }

    @Test
    void givenAVideoWithoutMedia_whenSave_thenLeaveNoRow() {
        this.videoGateway.create(VideoFixture.video());

        assertEquals(0, this.outboxRepository.count());
    }

    private Video videoWithMedia() {
        final var video = VideoFixture.video();
        video.updateVideoMedia(AudioVideoMedia.with("abc1", "duna.mp4", "raw/video"));
        return video;
    }
}
