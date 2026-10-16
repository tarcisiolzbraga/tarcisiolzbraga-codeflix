package com.tarcisiolzbraga.codeflix.subscription.application.subscription.overdue;

import java.util.List;

// O que a varredura fez, para o job registrar em log e para o teste afirmar.
//
// `refused` é cobrança que o provedor recusou: ela foi concluída, e o estado da assinatura mudou de
// acordo. `failed` é a que não chegou a ser concluída — conta sem meio de pagamento guardado, perda
// da corrida do lock otimista, provedor fora do ar.
//
// `failures` carrega o motivo de cada falha, **limitado**: um job que roda sozinho e conta falhas
// sem dizer o motivo é impossível de diagnosticar, e devolver uma lista do tamanho da base seria o
// oposto do que a paginação resolve. O limite está em MAX_REPORTED, e o contador segue completo.
//
// O caso de uso não loga nada: o módulo `application` depende só do `domain` e não conhece
// framework de log. Quem registra é o job, na infrastructure.
public record ChargeOverdueSubscriptionsOutput(
        int scanned, int charged, int refused, int failed, List<String> failures) {

    public static final int MAX_REPORTED = 10;

    public ChargeOverdueSubscriptionsOutput {
        failures = failures == null ? List.of() : List.copyOf(failures);
    }
}
