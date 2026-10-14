package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SubscriptionPropertiesTest {

    @Test
    void givenTheToleranceInDays_whenBindsTheProperties_thenKeepsIt() {
        final var actual = new SubscriptionProperties(5);

        assertEquals(5, actual.toleranceDays());
    }

    @Test
    void givenNoTolerance_whenBindsTheProperties_thenFallsBackToTwoDays() {
        final var actual = new SubscriptionProperties(null);

        assertEquals(2, actual.toleranceDays());
    }

    // Zero é valor legítimo: significa cancelar na primeira recusa.
    @Test
    void givenZeroDays_whenBindsTheProperties_thenAcceptsIt() {
        final var actual = new SubscriptionProperties(0);

        assertEquals(0, actual.toleranceDays());
    }

    @Test
    void givenANegativeTolerance_whenBindsTheProperties_thenRefusesToStart() {
        final var actual = assertThrows(IllegalStateException.class, () -> new SubscriptionProperties(-1));

        assertEquals("'subscription.tolerance-days' should not be negative", actual.getMessage());
    }
}
