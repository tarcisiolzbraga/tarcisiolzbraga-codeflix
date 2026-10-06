package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration.AmqpProperties;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.persistence.OutboxEventJpaEntity;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.persistence.OutboxEventRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class OutboxCleanerIT {

    @Autowired
    private OutboxCleaner cleaner;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private AmqpProperties amqpProperties;

    @Test
    void givenARowDeliveredLongAgo_whenRemoveDelivered_thenItIsGone() {
        givenDeliveredRow(olderThanRetention());

        final var actualRemoved = this.cleaner.removeDelivered();

        assertEquals(1, actualRemoved);
        assertEquals(0, this.outboxRepository.count());
    }

    @Test
    void givenARowDeliveredJustNow_whenRemoveDelivered_thenKeepIt() {
        givenDeliveredRow(InstantUtils.now());

        this.cleaner.removeDelivered();

        assertEquals(1, this.outboxRepository.count());
    }

    @Test
    void givenAnOldPendingRow_whenRemoveDelivered_thenKeepIt() {
        this.outboxRepository.saveAndFlush(
                OutboxEventJpaEntity.pending("video.created", "{}", olderThanRetention()));

        this.cleaner.removeDelivered();

        assertEquals(1, this.outboxRepository.count());
        assertTrue(this.outboxRepository.findAll().getFirst().getSentAt() == null);
    }

    private void givenDeliveredRow(final Instant sentAt) {
        final var row = OutboxEventJpaEntity.pending("video.created", "{}", olderThanRetention());
        row.markSent(sentAt);
        this.outboxRepository.saveAndFlush(row);
    }

    private Instant olderThanRetention() {
        return InstantUtils.now().minus(this.amqpProperties.outbox().retention()).minus(Duration.ofDays(1));
    }
}
