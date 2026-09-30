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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest
class RabbitDomainEventPublisherIT {

    private static final long RECEIVE_TIMEOUT = 5000L;

    @Autowired
    private VideoGateway videoGateway;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpProperties amqpProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenAVideoWithANewMedia_whenSave_thenPutTheEventOnTheQueue() {
        final var video = videoWithMedia();

        this.videoGateway.create(video);

        final var message = receive();
        assertNotNull(message);
        assertEquals(video.getId().getValue(), message.get("videoId"));
        assertEquals("VIDEO", message.get("type"));
        assertEquals("raw/video", message.get("filePath"));
        assertEquals("abc1", message.get("checksum"));
        assertNotNull(message.get("occurredOn"));
    }

    @Test
    void givenAnAlreadyPublishedVideo_whenSaveAgain_thenSendNoNewMessage() {
        final var video = this.videoGateway.create(videoWithMedia());
        drain();

        this.videoGateway.update(video);

        assertNull(this.rabbitTemplate.receiveAndConvert(queue(), 500L));
    }

    @Test
    void givenAVideoWithoutMedia_whenSave_thenSendNothing() {
        this.videoGateway.create(VideoFixture.video());

        assertNull(this.rabbitTemplate.receiveAndConvert(queue(), 500L));
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
