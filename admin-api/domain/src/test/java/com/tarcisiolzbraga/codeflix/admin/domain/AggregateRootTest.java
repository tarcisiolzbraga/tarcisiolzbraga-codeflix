package com.tarcisiolzbraga.codeflix.admin.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.events.DomainEvent;
import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AggregateRootTest {

    @Test
    void givenANewAggregate_whenCallGetDomainEvents_thenBeEmpty() {
        final var actualAggregate = new SampleAggregate();

        assertTrue(actualAggregate.getDomainEvents().isEmpty());
    }

    @Test
    void givenAnAggregate_whenRegisterAnEvent_thenKeepItUntilPublished() {
        final var actualAggregate = new SampleAggregate();
        final var expectedEvent = new SampleEvent();

        actualAggregate.doSomething(expectedEvent);

        assertEquals(List.of(expectedEvent), actualAggregate.getDomainEvents());
    }

    @Test
    void givenRegisteredEvents_whenCallPublishDomainEvents_thenHandThemOverAndForgetThem() {
        final var actualAggregate = new SampleAggregate();
        final var expectedEvent = new SampleEvent();
        actualAggregate.doSomething(expectedEvent);
        final var published = new ArrayList<DomainEvent>();

        actualAggregate.publishDomainEvents(published::add);

        assertEquals(List.of(expectedEvent), published);
        assertTrue(actualAggregate.getDomainEvents().isEmpty());
    }

    @Test
    void givenAnAggregate_whenChangeTheListItReceived_thenKeepItsOwnEvents() {
        final var actualAggregate = new SampleAggregate();
        actualAggregate.doSomething(new SampleEvent());
        final var received = actualAggregate.getDomainEvents();

        assertThrows(UnsupportedOperationException.class, received::clear);

        assertEquals(1, actualAggregate.getDomainEvents().size());
    }

    @Test
    void givenANullPublisher_whenCallPublishDomainEvents_thenReceiveAnError() {
        final var actualAggregate = new SampleAggregate();

        final var actualException =
                assertThrows(NullPointerException.class, () -> actualAggregate.publishDomainEvents(null));

        assertEquals("'publisher' should not be null", actualException.getMessage());
    }

    private record SampleId(UUID value) implements Identifier {
    }

    private record SampleEvent(Instant occurredOn) implements DomainEvent {

        private SampleEvent() {
            this(InstantUtils.now());
        }
    }

    private static final class SampleAggregate extends AggregateRoot<SampleId> {

        private SampleAggregate() {
            super(new SampleId(UUID.randomUUID()), true, InstantUtils.now(), InstantUtils.now());
        }

        @Override
        public void validate(final ValidationHandler handler) {
            // nada a validar: o agregado existe só para exercitar os eventos
        }

        private void doSomething(final DomainEvent event) {
            registerEvent(event);
        }
    }
}
