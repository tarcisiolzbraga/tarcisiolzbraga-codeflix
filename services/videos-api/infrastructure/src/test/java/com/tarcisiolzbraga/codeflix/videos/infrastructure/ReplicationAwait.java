package com.tarcisiolzbraga.codeflix.videos.infrastructure;

import static org.junit.jupiter.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.function.Supplier;
import org.awaitility.core.ConditionTimeoutException;
import org.awaitility.core.ThrowingRunnable;

// Espera a replicação chegar ao índice, e **diz o motivo** quando ela não chega.
//
// Os quatro E2E deste serviço têm o mesmo formato: publica no tópico, espera o índice refletir. E os
// quatro tiveram a mesma intermitência, que só aparece no CI, com a mesma mensagem inútil — um
// `AssertionFailedError` sem corpo, ou uma contagem sem contexto.
//
// Isto nasceu no GenreE2ETest, e virou peça comum quando o CI falhou no CastMemberE2ETest: a
// intermitência não é de um agregado, é do padrão de esperar replicação. Mantê-la num teste só
// deixaria os outros três cegos.
public final class ReplicationAwait {

    private ReplicationAwait() {}

    public static void awaitReplicated(
            final Duration atMost,
            final ListenerLogCapture listenerLog,
            final String expectation,
            final Supplier<String> indexState,
            final ThrowingRunnable assertion) {
        awaitDuring(null, atMost, listenerLog, expectation, indexState, assertion);
    }

    // A variante com `during`: exige que a condição se mantenha por um tempo antes de valer. Um
    // teste usa isso para provar que algo **não** muda — e ali o diagnóstico é igualmente necessário.
    public static void awaitDuring(
            final Duration during,
            final Duration atMost,
            final ListenerLogCapture listenerLog,
            final String expectation,
            final Supplier<String> indexState,
            final ThrowingRunnable assertion) {
        try {
            final var condicao = during == null ? await() : await().during(during);
            condicao.atMost(atMost).untilAsserted(assertion);
        } catch (final ConditionTimeoutException timeout) {
            // O estado do índice é lido **depois** do tempo esgotar, e não antes: é o que estava lá
            // quando a espera desistiu, que é a informação que falta.
            fail("""
                    %s não aconteceu em %s.
                    O índice tem: %s
                    O que os listeners registraram (WARN e acima): %s"""
                    .formatted(
                            expectation,
                            atMost,
                            safely(indexState),
                            describe(listenerLog)));
        }
    }

    // Ler o índice pode estourar — é a mesma rede que acabou de não responder. Uma exceção aqui
    // trocaria a mensagem de diagnóstico por um rastro de pilha, e o motivo se perderia de novo.
    private static String safely(final Supplier<String> indexState) {
        try {
            return indexState.get();
        } catch (final RuntimeException failure) {
            return "não foi possível ler: %s: %s".formatted(failure.getClass().getSimpleName(), failure.getMessage());
        }
    }

    private static String describe(final ListenerLogCapture listenerLog) {
        final var avisos = listenerLog.warningsAndErrors();
        return avisos.isEmpty() ? "nada — nem aviso de gateway vazio, nem fila morta" : avisos.toString();
    }
}
