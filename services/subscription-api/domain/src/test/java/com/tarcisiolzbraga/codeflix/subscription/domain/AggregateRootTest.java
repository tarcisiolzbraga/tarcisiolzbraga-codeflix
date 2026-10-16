package com.tarcisiolzbraga.codeflix.subscription.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AggregateRootTest {

    private static final String AGGREGATE_TYPE = "Stub";

    @Test
    void givenNewAggregate_whenAsksForItsEvents_thenThereIsNone() {
        final var aggregate = new StubAggregate(anId(), 0);

        final var actual = aggregate.domainEvents();

        assertTrue(actual.isEmpty());
    }

    @Test
    void givenAnAggregate_whenItRegistersAnEvent_thenTheEventIsListed() {
        final var aggregate = new StubAggregate(anId(), 0);
        final var event = anEventOf(aggregate);

        aggregate.record(event);

        assertEquals(List.of(event), aggregate.domainEvents());
    }

    @Test
    void givenAnAggregate_whenItRegistersNull_thenThrowsDomainException() {
        final var aggregate = new StubAggregate(anId(), 0);

        final var actual = assertThrows(RuntimeException.class, () -> aggregate.record(null));

        assertEquals("'event' should not be null", actual.getMessage());
    }

    @Test
    void givenAnAggregateWithEvents_whenTheCallerMutatesTheReturnedList_thenTheAggregateIsUntouched() {
        final var aggregate = new StubAggregate(anId(), 0);
        aggregate.record(anEventOf(aggregate));
        final var events = aggregate.domainEvents();

        assertThrows(UnsupportedOperationException.class, events::clear);

        assertEquals(1, aggregate.domainEvents().size());
    }

    @Test
    void givenAnAggregateWithEvents_whenItPublishesThem_thenThePublisherGetsAllOfThem() {
        final var aggregate = new StubAggregate(anId(), 0);
        final var event = anEventOf(aggregate);
        aggregate.record(event);
        final var delivered = new ArrayList<DomainEvent>();

        aggregate.publishDomainEvents(delivered::add);

        assertEquals(List.of(event), delivered);
    }

    @Test
    void givenAnAggregateWithEvents_whenItPublishesThem_thenNoneIsLeftToPublishAgain() {
        final var aggregate = new StubAggregate(anId(), 0);
        aggregate.record(anEventOf(aggregate));

        aggregate.publishDomainEvents(event -> {});

        assertTrue(aggregate.domainEvents().isEmpty());
    }

    @Test
    void givenAnAggregateReadFromStorage_whenAsksForItsVersion_thenItIsTheStoredOne() {
        final var aggregate = new StubAggregate(anId(), 7);

        final var actual = aggregate.version();

        assertEquals(7, actual);
    }

    private static StubId anId() {
        return new StubId(UUID.randomUUID());
    }

    private static StubEvent anEventOf(final StubAggregate aggregate) {
        return new StubEvent(aggregate.id().getValue());
    }

    private record StubId(UUID value) implements Identifier {}

    private record StubEvent(String aggregateId) implements DomainEvent {
        @Override
        public Instant occurredOn() {
            return Instant.EPOCH;
        }

        @Override
        public String aggregateType() {
            return AGGREGATE_TYPE;
        }
    }

    private static final class StubAggregate extends AggregateRoot<StubId> {
        private StubAggregate(final StubId id, final int version) {
            super(id, version);
        }

        private void record(final DomainEvent event) {
            registerEvent(event);
        }
    }
}
