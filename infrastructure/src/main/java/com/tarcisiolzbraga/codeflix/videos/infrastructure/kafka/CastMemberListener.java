package com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka;

import com.tarcisiolzbraga.codeflix.videos.application.castmember.delete.DeleteCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.castmember.save.SaveCastMemberCommand;
import com.tarcisiolzbraga.codeflix.videos.application.castmember.save.SaveCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.CastMemberClient;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models.CastMemberDTO;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models.CastMemberEvent;
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

// Replica no catálogo o que mudou na tabela cast_member do admin-codeflix, com o mesmo roteamento do
// listener da categoria: o CDC avisa que mudou, o REST diz o que é.
@Component
public class CastMemberListener {

    private static final Logger LOG = LoggerFactory.getLogger(CastMemberListener.class);
    private static final TypeReference<MessageValue<CastMemberEvent>> MESSAGE = new TypeReference<>() {};

    private final ObjectMapper mapper;
    private final CastMemberClient castMemberClient;
    private final SaveCastMemberUseCase saveCastMemberUseCase;
    private final DeleteCastMemberUseCase deleteCastMemberUseCase;

    public CastMemberListener(
            final ObjectMapper mapper,
            final CastMemberClient castMemberClient,
            final SaveCastMemberUseCase saveCastMemberUseCase,
            final DeleteCastMemberUseCase deleteCastMemberUseCase) {
        this.mapper = Objects.requireNonNull(mapper, "'mapper' should not be null");
        this.castMemberClient = Objects.requireNonNull(castMemberClient, "'castMemberClient' should not be null");
        this.saveCastMemberUseCase =
                Objects.requireNonNull(saveCastMemberUseCase, "'saveCastMemberUseCase' should not be null");
        this.deleteCastMemberUseCase =
                Objects.requireNonNull(deleteCastMemberUseCase, "'deleteCastMemberUseCase' should not be null");
    }

    @KafkaListener(
            id = "${kafka.consumers.cast-member.id}",
            topics = "${kafka.consumers.cast-member.topics}",
            groupId = "${kafka.consumers.cast-member.group-id}",
            concurrency = "${kafka.consumers.cast-member.concurrency}",
            properties = {"auto.offset.reset=${kafka.consumers.cast-member.auto-offset-reset}"})
    @RetryableTopic(
            attempts = "${kafka.consumers.cast-member.max-attempts}",
            backOff = @BackOff(delay = 1000, multiplier = 2),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    public void onMessage(
            @Payload(required = false) final String payload, final ConsumerRecordMetadata metadata) {
        // Payload nulo é o tombstone que o Debezium publica depois de cada remoção, para a
        // compactação de log: o evento "d" já veio antes, e desserializar nulo derrubaria a partição.
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

    // Só registra: reprocessar aqui a mensagem que já falhou todas as tentativas a faria falhar de
    // novo. A fila morta existe para alguém olhar.
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
            message.beforeState().map(CastMemberEvent::id).ifPresent(this::remove);
        } else if (operation.carriesNewState()) {
            message.afterState().map(CastMemberEvent::id).ifPresent(this::replicate);
        } else {
            LOG.warn("operação {} não traz linha; mensagem ignorada", operation);
        }
    }

    private void remove(final String id) {
        this.deleteCastMemberUseCase.execute(CastMemberID.from(id));
    }

    private void replicate(final String id) {
        this.castMemberClient
                .castMemberOfId(id)
                .ifPresentOrElse(
                        dto -> this.saveCastMemberUseCase.execute(commandOf(dto)),
                        () -> LOG.warn("membro de elenco {} não está mais no admin-codeflix; nada a replicar", id));
    }

    private static SaveCastMemberCommand commandOf(final CastMemberDTO dto) {
        return new SaveCastMemberCommand(
                dto.id(), dto.name(), dto.type(), dto.active(), dto.createdAt(), dto.updatedAt());
    }
}
