package com.tarcisiolzbraga.codeflix.subscription.domain.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import org.junit.jupiter.api.Test;

// O resultado é um ou outro, e o tipo garante isso: sucesso não carrega motivo de falha, e falha
// sempre carrega um. Na referência é um record só, com um booleano e um motivo que pode ser nulo —
// e aí "aprovado com motivo de recusa" é construível.
class TransactionTest {

    private static final String TRANSACTION_ID = "tx-0001";

    @Test
    void givenASuccess_whenCreated_thenItCarriesTheTransactionId() {
        final var actual = new Transaction.Succeeded(TRANSACTION_ID);

        assertEquals(TRANSACTION_ID, actual.transactionId());
    }

    @Test
    void givenAFailure_whenCreated_thenItCarriesTheReason() {
        final var actual = new Transaction.Failed(TRANSACTION_ID, "cartão recusado");

        assertEquals("cartão recusado", actual.reason());
        assertEquals(TRANSACTION_ID, actual.transactionId());
    }

    // O provedor pode recusar antes de abrir transação: aí não há id, e isso é um caso de verdade.
    @Test
    void givenAFailureWithNoTransaction_whenCreated_thenItIsStillValid() {
        final var actual = new Transaction.Failed(null, "comunicação com o provedor falhou");

        assertEquals("comunicação com o provedor falhou", actual.reason());
    }

    @Test
    void givenASuccessWithoutAnId_whenCreated_thenThrowsDomainException() {
        final var actual = assertThrows(DomainException.class, () -> new Transaction.Succeeded("  "));

        assertEquals("'transactionId' should not be empty", actual.getMessage());
    }

    @Test
    void givenAFailureWithoutAReason_whenCreated_thenThrowsDomainException() {
        final var actual = assertThrows(DomainException.class, () -> new Transaction.Failed(TRANSACTION_ID, " "));

        assertEquals("'reason' should not be empty", actual.getMessage());
    }

    @Test
    void givenTheTwoOutcomes_whenMatched_thenTheCompilerCoversBoth() {
        final Transaction actual = new Transaction.Succeeded(TRANSACTION_ID);

        final var matched = switch (actual) {
            case Transaction.Succeeded s -> "aprovada " + s.transactionId();
            case Transaction.Failed f -> "recusada " + f.reason();
        };

        assertTrue(matched.startsWith("aprovada"));
    }
}
