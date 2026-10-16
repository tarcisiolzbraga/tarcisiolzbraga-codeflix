package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SubscriptionPropertiesTest {

    @Test
    void givenTheToleranceInDays_whenBindsTheProperties_thenKeepsIt() {
        final var actual = new SubscriptionProperties(5, null);

        assertEquals(5, actual.toleranceDays());
    }

    @Test
    void givenNoTolerance_whenBindsTheProperties_thenFallsBackToTwoDays() {
        final var actual = new SubscriptionProperties(null, null);

        assertEquals(2, actual.toleranceDays());
    }

    // Zero é valor legítimo: significa cancelar na primeira recusa.
    @Test
    void givenZeroDays_whenBindsTheProperties_thenAcceptsIt() {
        final var actual = new SubscriptionProperties(0, null);

        assertEquals(0, actual.toleranceDays());
    }

    @Test
    void givenANegativeTolerance_whenBindsTheProperties_thenRefusesToStart() {
        final var actual = assertThrows(IllegalStateException.class, () -> new SubscriptionProperties(-1, null));

        assertEquals("'subscription.tolerance-days' should not be negative", actual.getMessage());
    }

    // O cron e o lote têm padrão, para o serviço subir sem ninguém configurar a varredura.
    @Test
    void givenNoChargeJobSettings_whenBindsTheProperties_thenFallsBackToNightlyAndAHundred() {
        final var actual = new SubscriptionProperties(null, null);

        assertEquals("0 0 3 * * *", actual.chargeJob().cron());
        assertEquals(100, actual.chargeJob().batchSize());
    }

    @Test
    void givenABlankCron_whenBindsTheProperties_thenFallsBackToTheDefault() {
        final var actual = new SubscriptionProperties(2, new SubscriptionProperties.ChargeJob("  ", 50));

        assertEquals("0 0 3 * * *", actual.chargeJob().cron());
        assertEquals(50, actual.chargeJob().batchSize());
    }

    // Lote zero faria a varredura ler uma página vazia e terminar sem olhar ninguém, em silêncio.
    //
    // A construção fica DENTRO do lambda: quem recusa é o construtor do ChargeJob, então montá-lo
    // fora faria a exceção escapar do assertThrows e o teste falhar por não ter sido escrito certo.
    @Test
    void givenANonPositiveBatchSize_whenBindsTheProperties_thenRefusesToStart() {
        final var actual = assertThrows(
                IllegalStateException.class, () -> new SubscriptionProperties.ChargeJob(null, 0));

        assertEquals("'subscription.charge-job.batch-size' should be positive", actual.getMessage());
    }
}
