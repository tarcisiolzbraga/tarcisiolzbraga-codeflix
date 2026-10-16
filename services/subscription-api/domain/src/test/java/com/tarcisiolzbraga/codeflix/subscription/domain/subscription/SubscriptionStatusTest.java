package com.tarcisiolzbraga.codeflix.subscription.domain.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

// A tabela de transições, exercitada por inteiro. Isto é o que o desenho sem referência de volta ao
// agregado compra: o estado se testa sozinho, sem montar uma assinatura.
class SubscriptionStatusTest {

    @Test
    void givenATrailingSubscription_whenTheChargeSucceeds_thenItBecomesActive() {
        final var actual = SubscriptionStatus.TRAILING.activate();

        assertEquals(SubscriptionStatus.ACTIVE, actual);
    }

    @Test
    void givenATrailingSubscription_whenTheChargeFails_thenItBecomesIncomplete() {
        final var actual = SubscriptionStatus.TRAILING.markIncomplete();

        assertEquals(SubscriptionStatus.INCOMPLETE, actual);
    }

    @Test
    void givenAnIncompleteSubscription_whenTheRetrySucceeds_thenItBecomesActive() {
        final var actual = SubscriptionStatus.INCOMPLETE.activate();

        assertEquals(SubscriptionStatus.ACTIVE, actual);
    }

    @Test
    void givenAnActiveSubscription_whenTheChargeFails_thenItBecomesIncomplete() {
        final var actual = SubscriptionStatus.ACTIVE.markIncomplete();

        assertEquals(SubscriptionStatus.INCOMPLETE, actual);
    }

    // Repetir a transição que já aconteceu não é erro: a cobrança pode ser tentada de novo, e o
    // resultado igual não deve derrubar nada.
    @Test
    void givenAnActiveSubscription_whenActivatedAgain_thenItStaysActive() {
        final var actual = SubscriptionStatus.ACTIVE.activate();

        assertEquals(SubscriptionStatus.ACTIVE, actual);
    }

    @Test
    void givenAnIncompleteSubscription_whenMarkedIncompleteAgain_thenItStaysIncomplete() {
        final var actual = SubscriptionStatus.INCOMPLETE.markIncomplete();

        assertEquals(SubscriptionStatus.INCOMPLETE, actual);
    }

    // Cancelar vale de qualquer estado: é o que o assinante pede, e o que a cobrança faz quando o
    // prazo de tolerância acaba.
    @ParameterizedTest
    @EnumSource(SubscriptionStatus.class)
    void givenAnyStatus_whenCanceled_thenItBecomesCanceled(final SubscriptionStatus status) {
        final var actual = status.cancel();

        assertEquals(SubscriptionStatus.CANCELED, actual);
    }

    // Cancelado é terminal. É a regra que protege o assinante de voltar a ser cobrado por uma
    // assinatura que ele encerrou.
    @Test
    void givenACanceledSubscription_whenActivated_thenRefuses() {
        final var actual = assertThrows(DomainException.class, SubscriptionStatus.CANCELED::activate);

        assertEquals("a canceled subscription cannot become active", actual.getMessage());
    }

    @Test
    void givenACanceledSubscription_whenMarkedIncomplete_thenRefuses() {
        final var actual = assertThrows(DomainException.class, SubscriptionStatus.CANCELED::markIncomplete);

        assertEquals("a canceled subscription cannot become incomplete", actual.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"trailing", "active", "incomplete", "canceled"})
    void givenTheStoredText_whenRebuilds_thenItRoundTrips(final String stored) {
        final var actual = SubscriptionStatus.of(stored);

        assertEquals(stored, actual.value());
    }

    @Test
    void givenTextThatIsNoStatus_whenRebuilds_thenThrowsDomainException() {
        final var actual = assertThrows(DomainException.class, () -> SubscriptionStatus.of("pausada"));

        assertEquals("'pausada' is not a valid subscription status", actual.getMessage());
    }

    @Test
    void givenNull_whenRebuilds_thenThrowsDomainException() {
        final var actual = assertThrows(DomainException.class, () -> SubscriptionStatus.of(null));

        assertEquals("'status' should not be empty", actual.getMessage());
    }

    // A coluna guarda o texto minúsculo, e é o mesmo do curso: o banco de quem seguiu a referência
    // continua legível por este código.
    @ParameterizedTest
    @EnumSource(SubscriptionStatus.class)
    void givenAnyStatus_whenAsksItsStoredForm_thenItIsTheLowerCaseName(final SubscriptionStatus status) {
        final var actual = status.value();

        assertEquals(status.name().toLowerCase(Locale.ROOT), actual);
    }
}
