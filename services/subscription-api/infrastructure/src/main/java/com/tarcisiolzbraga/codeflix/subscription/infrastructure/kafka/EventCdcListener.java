package com.tarcisiolzbraga.codeflix.subscription.infrastructure.kafka;

import com.tarcisiolzbraga.codeflix.subscription.application.account.access.GrantCatalogAccessUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.access.RevokeCatalogAccessUseCase;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.kafka.models.connect.MessageValue;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.kafka.models.connect.Operation;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.kafka.models.event.EventMsg;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.kafka.models.event.SubscriptionEventPayload;
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

// Lê a fila de saída pelo CDC e dá consequência aos eventos de domínio.
//
// Hoje a consequência é uma só, e é a que fecha o acesso ao catálogo: assinatura que começa ou é
// renovada concede, assinatura cancelada revoga. Incompleta não mexe — a tolerância existe para a
// pessoa resolver o cartão sem perder o serviço no meio.
//
// Sem o padrão observador da referência (Publisher, Subscriber, test(), onEvent()): é maquinário
// para despachar entre dois handlers, e um `switch` sobre o tipo do evento diz a mesma coisa num
// lugar só. Quando houver muitos consumidores por evento, a conversa muda.
//
// Só a criação de linha interessa: a fila de saída é append-only, então UPDATE e DELETE nela só
// aconteceriam por intervenção à mão — e reagir a isso daria consequência a algo que não é fato.
@Component
public class EventCdcListener {

    private static final Logger LOG = LoggerFactory.getLogger(EventCdcListener.class);
    private static final TypeReference<MessageValue<EventMsg>> MESSAGE = new TypeReference<>() {};

    private static final String SUBSCRIPTION_CREATED = "SubscriptionCreated";
    private static final String SUBSCRIPTION_RENEWED = "SubscriptionRenewed";
    private static final String SUBSCRIPTION_CANCELED = "SubscriptionCanceled";

    private final ObjectMapper mapper;
    private final GrantCatalogAccessUseCase grantCatalogAccessUseCase;
    private final RevokeCatalogAccessUseCase revokeCatalogAccessUseCase;

    public EventCdcListener(
            final ObjectMapper mapper,
            final GrantCatalogAccessUseCase grantCatalogAccessUseCase,
            final RevokeCatalogAccessUseCase revokeCatalogAccessUseCase) {
        this.mapper = Objects.requireNonNull(mapper, "'mapper' should not be null");
        this.grantCatalogAccessUseCase =
                Objects.requireNonNull(grantCatalogAccessUseCase, "'grantCatalogAccessUseCase' should not be null");
        this.revokeCatalogAccessUseCase =
                Objects.requireNonNull(revokeCatalogAccessUseCase, "'revokeCatalogAccessUseCase' should not be null");
    }

    @KafkaListener(
            id = "${kafka.consumers.events.id}",
            topics = "${kafka.consumers.events.topics}",
            groupId = "${kafka.consumers.events.group-id}",
            concurrency = "${kafka.consumers.events.concurrency}",
            properties = {"auto.offset.reset=${kafka.consumers.events.auto-offset-reset}"})
    @RetryableTopic(
            attempts = "${kafka.consumers.events.max-attempts}",
            // **As partições do tópico, e por que isto fica aqui.** Quem cria os tópicos é esta
            // anotação, na subida — não o Debezium e não o broker. Medido: apagar um tópico e deixar
            // o broker recriá-lo dava 3 partições, enquanto a subida da aplicação dava 1. Então o
            // padrão do broker não alcança estes, e a contagem tem de ser dita aqui.
            //
            // É o que permite mais de uma réplica consumir: com uma partição, a segunda fica ociosa.
            numPartitions = "${kafka.topic-partitions}",
            backOff = @BackOff(delay = 1000, multiplier = 2),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    public void onMessage(
            @Payload(required = false) final String payload, final ConsumerRecordMetadata metadata) {
        // Payload nulo é o tombstone que o Debezium publica depois de cada remoção, para a
        // compactação de log. Desserializar nulo derrubaria o consumo da partição.
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

    // O DLT só registra. Reprocessar aqui a mensagem que já falhou todas as tentativas a faria
    // falhar de novo: a fila morta existe para alguém olhar, não para tentar uma vez mais.
    @DltHandler
    public void onDeadLetter(
            @Payload(required = false) final String payload, final ConsumerRecordMetadata metadata) {
        LOG.error(
                "evento descartado para a fila morta em {}-{} offset {}: {}",
                metadata.topic(),
                metadata.partition(),
                metadata.offset(),
                payload);
    }

    private void handle(final String message) {
        final var body = this.mapper.readValue(message, MESSAGE).payload();
        final var operation = body.operation().orElse(null);
        if (!isNewRow(operation)) {
            LOG.debug("operação {} na fila de saída; nada a fazer", operation);
            return;
        }
        body.afterState().ifPresent(this::dispatch);
    }

    // CREATE e READ, e não o `carriesNewState()` do enum, que inclui UPDATE: a fila de saída é
    // append-only, então UPDATE nela só aconteceria por intervenção à mão, e dar consequência a isso
    // daria consequência a algo que não é fato novo. READ entra porque é com ele que o Debezium
    // publica cada linha no snapshot inicial.
    private static boolean isNewRow(final Operation operation) {
        return operation == Operation.CREATE || operation == Operation.READ;
    }

    private void dispatch(final EventMsg event) {
        switch (event.eventType()) {
            case SUBSCRIPTION_CREATED, SUBSCRIPTION_RENEWED -> grant(event);
            case SUBSCRIPTION_CANCELED -> revoke(event);
            // Evento que ninguém consome não é erro: o agregado registra o fato, e quem tem
            // interesse nele aparece depois. Mandá-lo para a fila morta por isso pararia o consumo.
            default -> LOG.debug("evento {} sem consumidor; ignorado", event.eventType());
        }
    }

    private void grant(final EventMsg event) {
        final var accountId = accountIdOf(event);
        LOG.info("concedendo catálogo à conta {} por {}", accountId, event.eventType());
        this.grantCatalogAccessUseCase.execute(accountId);
    }

    private void revoke(final EventMsg event) {
        final var accountId = accountIdOf(event);
        LOG.info("revogando catálogo da conta {} por {}", accountId, event.eventType());
        this.revokeCatalogAccessUseCase.execute(accountId);
    }

    private String accountIdOf(final EventMsg event) {
        return this.mapper.readValue(event.payload(), SubscriptionEventPayload.class).accountId();
    }
}
