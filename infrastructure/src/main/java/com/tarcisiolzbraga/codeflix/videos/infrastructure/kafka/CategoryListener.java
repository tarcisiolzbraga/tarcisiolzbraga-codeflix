package com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka;

import com.tarcisiolzbraga.codeflix.videos.application.category.delete.DeleteCategoryUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.category.save.SaveCategoryCommand;
import com.tarcisiolzbraga.codeflix.videos.application.category.save.SaveCategoryUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.CategoryClient;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.CategoryDTO;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.CategoryEvent;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka.models.connect.MessageValue;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka.models.connect.Operation;
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

// Replica no catálogo o que mudou na tabela category do admin-codeflix.
//
// O evento do CDC só diz qual id mudou; o registro completo vem da API REST do admin. Por isso o
// par listener + cliente: o CDC avisa que mudou, o REST diz o que é.
@Component
public class CategoryListener {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryListener.class);
    private static final TypeReference<MessageValue<CategoryEvent>> MESSAGE = new TypeReference<>() {};

    private final ObjectMapper mapper;
    private final CategoryClient categoryClient;
    private final SaveCategoryUseCase saveCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    public CategoryListener(
            final ObjectMapper mapper,
            final CategoryClient categoryClient,
            final SaveCategoryUseCase saveCategoryUseCase,
            final DeleteCategoryUseCase deleteCategoryUseCase) {
        this.mapper = Objects.requireNonNull(mapper, "'mapper' should not be null");
        this.categoryClient = Objects.requireNonNull(categoryClient, "'categoryClient' should not be null");
        this.saveCategoryUseCase =
                Objects.requireNonNull(saveCategoryUseCase, "'saveCategoryUseCase' should not be null");
        this.deleteCategoryUseCase =
                Objects.requireNonNull(deleteCategoryUseCase, "'deleteCategoryUseCase' should not be null");
    }

    @KafkaListener(
            id = "${kafka.consumers.category.id}",
            topics = "${kafka.consumers.category.topics}",
            groupId = "${kafka.consumers.category.group-id}",
            concurrency = "${kafka.consumers.category.concurrency}",
            properties = {"auto.offset.reset=${kafka.consumers.category.auto-offset-reset}"})
    @RetryableTopic(
            attempts = "${kafka.consumers.category.max-attempts}",
            // O Spring Kafka 4 não usa mais o spring-retry: o @BackOff é o dele, e o atributo é
            // backOff, com O maiúsculo.
            backOff = @BackOff(delay = 1000, multiplier = 2),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    public void onMessage(
            @Payload(required = false) final String payload, final ConsumerRecordMetadata metadata) {
        // Payload nulo é o tombstone que o Debezium publica depois de cada remoção, para a
        // compactação de log. O evento "d" já veio na mensagem anterior, então aqui não há o que
        // fazer — e desserializar nulo derrubaria o consumo da partição.
        if (payload == null) {
            LOG.debug("tombstone recebido em {}-{} offset {}", metadata.topic(), metadata.partition(), metadata.offset());
            return;
        }
        handle(payload);
    }

    // O DLT só registra. Reprocessar aqui a mensagem que já falhou todas as tentativas a faria
    // falhar de novo: a fila morta existe para alguém olhar, não para tentar uma vez mais.
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
            message.beforeState().map(CategoryEvent::id).ifPresent(this::remove);
        } else if (operation.carriesNewState()) {
            message.afterState().map(CategoryEvent::id).ifPresent(this::replicate);
        } else {
            // TRUNCATE: não traz before nem after, então não há id para agir sobre.
            LOG.warn("operação {} não traz linha; mensagem ignorada", operation);
        }
    }

    private void remove(final String id) {
        this.deleteCategoryUseCase.execute(CategoryID.from(id));
    }

    private void replicate(final String id) {
        this.categoryClient
                .categoryOfId(id)
                .ifPresentOrElse(
                        dto -> this.saveCategoryUseCase.execute(commandOf(dto)),
                        // Não é erro: a categoria pode ter sido apagada no admin entre o evento e
                        // esta busca, e o evento de remoção vem logo atrás.
                        () -> LOG.warn("categoria {} não está mais no admin-codeflix; nada a replicar", id));
    }

    private static SaveCategoryCommand commandOf(final CategoryDTO dto) {
        return new SaveCategoryCommand(
                dto.id(), dto.name(), dto.description(), dto.active(), dto.createdAt(), dto.updatedAt());
    }
}
