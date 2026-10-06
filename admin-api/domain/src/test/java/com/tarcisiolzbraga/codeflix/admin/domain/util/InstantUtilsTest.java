package com.tarcisiolzbraga.codeflix.admin.domain.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class InstantUtilsTest {

    private static final int NANOS_PER_MICRO = 1_000;

    @Test
    void givenSystemClock_whenCallNow_thenTruncateToMicroseconds() {
        final var before = Instant.now().minusMillis(1);

        final var actualInstant = InstantUtils.now();

        assertEquals(0, actualInstant.getNano() % NANOS_PER_MICRO);
        assertFalse(actualInstant.isBefore(before));
    }
}
