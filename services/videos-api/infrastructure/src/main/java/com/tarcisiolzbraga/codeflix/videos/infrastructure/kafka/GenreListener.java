package com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka;

import com.tarcisiolzbraga.codeflix.videos.application.genre.delete.DeleteGenreUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.genre.save.SaveGenreCommand;
import com.tarcisiolzbraga.codeflix.videos.application.genre.save.SaveGenreUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.GenreClient;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models.GenreDTO;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models.GenreEvent;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka.models.connect.MessageValue;
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

// Replica no catálogo o que mudou na tabela genre do admin-codeflix.
//
// Aqui o par listener + cliente REST é mais que conveniência: a tabela genre_category não é
// capturada pelo CDC, então o vínculo com as categorias só existe na resposta da API. O evento avisa
// que o gênero mudou — inclusive quando o que mudou foi um vínculo, porque o admin toca a linha do
// genre ao mexer nele — e a chamada REST diz quais categorias ele tem agora.
@Component
public class GenreListener {

    private static final Logger LOG = LoggerFactory.getLogger(GenreListener.class);
    private static final TypeReference<MessageValue<GenreEvent>> MESSAGE = new TypeReference<>() {};

    private final ObjectMapper mapper;
    private final GenreClient genreClient;
    private final SaveGenreUseCase saveGenreUseCase;
    private final DeleteGenreUseCase deleteGenreUseCase;

    public GenreListener(
            final ObjectMapper mapper,
            final GenreClient genreClient,
            final SaveGenreUseCase saveGenreUseCase,
            final DeleteGenreUseCase deleteGenreUseCase) {
        this.mapper = Objects.requireNonNull(mapper, "'mapper' should not be null");
        this.genreClient = Objects.requireNonNull(genreClient, "'genreClient' should not be null");
        this.saveGenreUseCase = Objects.requireNonNull(saveGenreUseCase, "'saveGenreUseCase' should not be null");
        this.deleteGenreUseCase =
                Objects.requireNonNull(deleteGenreUseCase, "'deleteGenreUseCase' should not be null");
    }

    @KafkaListener(
            id = "${kafka.consumers.genre.id}",
            topics = "${kafka.consumers.genre.topics}",
            groupId = "${kafka.consumers.genre.group-id}",
            concurrency = "${kafka.consumers.genre.concurrency}",
            properties = {"auto.offset.reset=${kafka.consumers.genre.auto-offset-reset}"})
    @RetryableTopic(
            attempts = "${kafka.consumers.genre.max-attempts}",
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
            message.beforeState().map(GenreEvent::id).ifPresent(this::remove);
        } else if (operation.carriesNewState()) {
            message.afterState().map(GenreEvent::id).ifPresent(this::replicate);
        } else {
            LOG.warn("operação {} não traz linha; mensagem ignorada", operation);
        }
    }

    private void remove(final String id) {
        this.deleteGenreUseCase.execute(GenreID.from(id));
    }

    private void replicate(final String id) {
        this.genreClient
                .genreOfId(id)
                .ifPresentOrElse(
                        dto -> this.saveGenreUseCase.execute(commandOf(dto)),
                        () -> LOG.warn("gênero {} não está mais no admin-codeflix; nada a replicar", id));
    }

    private static SaveGenreCommand commandOf(final GenreDTO dto) {
        return new SaveGenreCommand(
                dto.id(), dto.name(), dto.active(), dto.categories(), dto.createdAt(), dto.updatedAt());
    }
}
