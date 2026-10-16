package com.tarcisiolzbraga.codeflix.subscription.infrastructure.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tarcisiolzbraga.codeflix.subscription.application.account.access.GrantCatalogAccessUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.access.RevokeCatalogAccessUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.adapter.ConsumerRecordMetadata;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.record.TimestampType;
import tools.jackson.databind.json.JsonMapper;

// O despacho do consumidor, sem broker: o que cada tipo de evento faz, e o que ele NÃO faz.
//
// A mensagem é montada no formato que o Debezium entrega de verdade — envelope com schema e
// payload, operação em "op", a linha em "after" — porque é a forma que o consumidor tem de aguentar.
@ExtendWith(MockitoExtension.class)
class EventCdcListenerTest {

    private static final String ACCOUNT_ID = "11111111-1111-1111-1111-111111111111";
    private static final String SUBSCRIPTION_ID = "22222222-2222-2222-2222-222222222222";

    @Mock
    private GrantCatalogAccessUseCase grantCatalogAccessUseCase;

    @Mock
    private RevokeCatalogAccessUseCase revokeCatalogAccessUseCase;

    @Test
    void givenASubscriptionCreated_whenConsumes_thenGrantsTheCatalogToThatAccount() {
        listener().onMessage(messageOf("c", "SubscriptionCreated"), metadata());

        verify(grantCatalogAccessUseCase).execute(ACCOUNT_ID);
        verify(revokeCatalogAccessUseCase, never()).execute(any());
    }

    @Test
    void givenASubscriptionRenewed_whenConsumes_thenGrantsTheCatalogAgain() {
        listener().onMessage(messageOf("c", "SubscriptionRenewed"), metadata());

        verify(grantCatalogAccessUseCase).execute(ACCOUNT_ID);
    }

    @Test
    void givenASubscriptionCanceled_whenConsumes_thenRevokesTheCatalog() {
        listener().onMessage(messageOf("c", "SubscriptionCanceled"), metadata());

        verify(revokeCatalogAccessUseCase).execute(ACCOUNT_ID);
        verify(grantCatalogAccessUseCase, never()).execute(any());
    }

    // Incompleta não mexe no catálogo: a tolerância existe para a pessoa resolver o cartão sem
    // perder o serviço no meio, e quem revoga é o cancelamento, que vem depois do prazo.
    @Test
    void givenASubscriptionIncomplete_whenConsumes_thenTheCatalogIsLeftAlone() {
        listener().onMessage(messageOf("c", "SubscriptionIncomplete"), metadata());

        verifyNoInteractions(grantCatalogAccessUseCase, revokeCatalogAccessUseCase);
    }

    // Evento que ninguém consome não é erro: o agregado registra o fato e quem se interessa aparece
    // depois. Mandá-lo para a fila morta pararia o consumo da partição.
    @Test
    void givenAnEventWithNoConsumer_whenConsumes_thenNothingHappensAndItDoesNotFail() {
        listener().onMessage(messageOf("c", "AccountCreated"), metadata());

        verifyNoInteractions(grantCatalogAccessUseCase, revokeCatalogAccessUseCase);
    }

    // A fila de saída é append-only: UPDATE nela só aconteceria por intervenção à mão, e dar
    // consequência a isso daria consequência a algo que não é fato novo.
    @Test
    void givenAnUpdateOnTheOutbox_whenConsumes_thenItIsIgnored() {
        listener().onMessage(messageOf("u", "SubscriptionCanceled"), metadata());

        verifyNoInteractions(grantCatalogAccessUseCase, revokeCatalogAccessUseCase);
    }

    @Test
    void givenADeleteOnTheOutbox_whenConsumes_thenItIsIgnored() {
        listener().onMessage(messageOf("d", "SubscriptionCanceled"), metadata());

        verifyNoInteractions(grantCatalogAccessUseCase, revokeCatalogAccessUseCase);
    }

    // Payload nulo é o tombstone que o Debezium publica depois de cada remoção. Desserializar nulo
    // derrubaria o consumo da partição inteira.
    @Test
    void givenATombstone_whenConsumes_thenItIsIgnoredWithoutFailing() {
        listener().onMessage(null, metadata());

        verifyNoInteractions(grantCatalogAccessUseCase, revokeCatalogAccessUseCase);
    }

    // Snapshot inicial: o Debezium publica cada linha já existente com "r", não com "c". Tratá-lo
    // como desconhecido faria a carga inicial passar sem consequência.
    @Test
    void givenASnapshotRead_whenConsumes_thenItIsTreatedAsANewRow() {
        listener().onMessage(messageOf("r", "SubscriptionCreated"), metadata());

        verify(grantCatalogAccessUseCase).execute(ACCOUNT_ID);
    }

    private EventCdcListener listener() {
        return new EventCdcListener(
                JsonMapper.builder().build(), grantCatalogAccessUseCase, revokeCatalogAccessUseCase);
    }

    private static String messageOf(final String operation, final String eventType) {
        final var payload = """
                {"aggregateId":"%s","accountId":"%s","occurredOn":"2026-10-14T12:00:00Z"}"""
                .formatted(SUBSCRIPTION_ID, ACCOUNT_ID);
        return """
                {"schema":{"type":"struct"},"payload":{"before":null,"after":{\
                "id":"33333333-3333-3333-3333-333333333333","aggregate_type":"Subscription",\
                "aggregate_id":"%s","event_type":"%s","occurred_on":1760443200000000,\
                "payload":%s},"source":{"db":"subscription","table":"event"},"op":"%s"}}"""
                .formatted(SUBSCRIPTION_ID, eventType, quoted(payload), operation);
    }

    // A coluna é JSON no MySQL, mas o Debezium a entrega como texto: o payload chega como string
    // escapada, e é assim que o consumidor tem de saber lê-la.
    private static String quoted(final String json) {
        return "\"" + json.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // Só serve ao log do consumidor; o conteúdo não entra em asserção nenhuma.
    private static ConsumerRecordMetadata metadata() {
        final var partition = new TopicPartition("subscription_mysql.subscription.event", 0);
        return new ConsumerRecordMetadata(
                new RecordMetadata(partition, 0L, 0, 0L, 0, 0), TimestampType.NO_TIMESTAMP_TYPE);
    }
}
