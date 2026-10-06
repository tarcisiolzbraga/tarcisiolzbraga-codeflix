package com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka;

import com.tarcisiolzbraga.codeflix.videos.application.video.delete.DeleteVideoUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.video.save.SaveVideoCommand;
import com.tarcisiolzbraga.codeflix.videos.application.video.save.SaveVideoUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.video.save.VideoDetailsCommand;
import com.tarcisiolzbraga.codeflix.videos.application.video.save.VideoFlagsCommand;
import com.tarcisiolzbraga.codeflix.videos.application.video.save.VideoMediasCommand;
import com.tarcisiolzbraga.codeflix.videos.application.video.save.VideoReferencesCommand;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka.models.connect.MessageValue;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.VideoClient;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.ImageMediaDTO;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.VideoDTO;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.VideoEvent;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.VideoMediaDTO;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.listener.adapter.ConsumerRecordMetadata;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// Replica no catálogo o que mudou na tabela video do admin-codeflix.
//
// É o agregado em que o par listener e cliente REST é mais indispensável: a linha do video não tem
// endereço de mídia nem relação alguma, só chaves estrangeiras. Tudo que o catálogo serve vem da
// resposta da API.
@Component
public class VideoListener {

    private static final Logger LOG = LoggerFactory.getLogger(VideoListener.class);
    private static final TypeReference<MessageValue<VideoEvent>> MESSAGE = new TypeReference<>() {};

    private final ObjectMapper mapper;
    private final VideoClient videoClient;
    private final SaveVideoUseCase saveVideoUseCase;
    private final DeleteVideoUseCase deleteVideoUseCase;

    public VideoListener(
            final ObjectMapper mapper,
            final VideoClient videoClient,
            final SaveVideoUseCase saveVideoUseCase,
            final DeleteVideoUseCase deleteVideoUseCase) {
        this.mapper = Objects.requireNonNull(mapper, "'mapper' should not be null");
        this.videoClient = Objects.requireNonNull(videoClient, "'videoClient' should not be null");
        this.saveVideoUseCase = Objects.requireNonNull(saveVideoUseCase, "'saveVideoUseCase' should not be null");
        this.deleteVideoUseCase =
                Objects.requireNonNull(deleteVideoUseCase, "'deleteVideoUseCase' should not be null");
    }

    @KafkaListener(
            id = "${kafka.consumers.video.id}",
            topics = "${kafka.consumers.video.topics}",
            groupId = "${kafka.consumers.video.group-id}",
            concurrency = "${kafka.consumers.video.concurrency}",
            properties = {"auto.offset.reset=${kafka.consumers.video.auto-offset-reset}"})
    @RetryableTopic(
            attempts = "${kafka.consumers.video.max-attempts}",
            backOff = @BackOff(delay = 1000, multiplier = 2),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    public void onMessage(
            @Payload(required = false) final String payload, final ConsumerRecordMetadata metadata) {
        if (payload == null) {
            LOG.debug(
                    "tombstone recebido em {}-{} offset {}",
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset());
            return;
        }
        handle(payload);
    }

    @DltHandler
    public void onDeadLetter(
            @Payload(required = false) final String payload, final ConsumerRecordMetadata metadata) {
        LOG.error(
                "mensagem descartada para a fila morta em {}-{} offset {}: {}",
                metadata.topic(),
                metadata.partition(),
                metadata.offset(),
                payload);
    }

    private void handle(final String payload) {
        final var message = this.mapper.readValue(payload, MESSAGE).payload();
        final var operation = message.operation().orElse(null);
        if (operation == null) {
            LOG.warn("operação desconhecida nesta versão; mensagem ignorada: {}", payload);
            return;
        }
        if (operation.isDelete()) {
            message.beforeState().map(VideoEvent::id).ifPresent(this::remove);
        } else if (operation.carriesNewState()) {
            message.afterState().map(VideoEvent::id).ifPresent(this::replicate);
        } else {
            LOG.warn("operação {} não traz linha; mensagem ignorada", operation);
        }
    }

    private void remove(final String id) {
        this.deleteVideoUseCase.execute(VideoID.from(id));
    }

    private void replicate(final String id) {
        this.videoClient
                .videoOfId(id)
                .ifPresentOrElse(
                        dto -> this.saveVideoUseCase.execute(commandOf(dto)),
                        () -> LOG.warn("vídeo {} não está mais no admin-codeflix; nada a replicar", id));
    }

    private static SaveVideoCommand commandOf(final VideoDTO dto) {
        return new SaveVideoCommand(
                dto.id(),
                new VideoDetailsCommand(
                        dto.title(),
                        dto.description(),
                        dto.launchedAt(),
                        dto.duration() == null ? 0 : dto.duration(),
                        dto.rating()),
                new VideoFlagsCommand(dto.opened(), dto.published(), dto.active()),
                new VideoMediasCommand(
                        addressOf(dto.video()),
                        addressOf(dto.trailer()),
                        addressOf(dto.banner()),
                        addressOf(dto.thumbnail()),
                        addressOf(dto.thumbnailHalf())),
                new VideoReferencesCommand(dto.categories(), dto.genres(), dto.castMembers()),
                dto.createdAt(),
                dto.updatedAt());
    }

    // Mídia ausente é estado normal: o vídeo existe no admin antes de qualquer arquivo ser enviado.
    private static String addressOf(final VideoMediaDTO media) {
        return media == null ? null : media.address();
    }

    private static String addressOf(final ImageMediaDTO media) {
        return media == null ? null : media.address();
    }
}
