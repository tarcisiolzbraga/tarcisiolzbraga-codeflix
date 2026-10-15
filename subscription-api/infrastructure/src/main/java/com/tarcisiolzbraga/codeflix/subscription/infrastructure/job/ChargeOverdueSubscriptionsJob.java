package com.tarcisiolzbraga.codeflix.subscription.infrastructure.job;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.overdue.ChargeOverdueSubscriptionsUseCase;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Dispara a varredura das vencidas. Só isso: a decisão de quem cobrar e o que fazer com o resultado
// é do caso de uso, e este job é o relógio batendo.
//
// **Duas instâncias do serviço rodam o job duas vezes**, e isso é aceitável sem lock distribuído
// porque as duas defesas já existem: o `orderId` da cobrança é estável, então o provedor reconhece
// a repetição e não cobra em dobro, e o lock otimista faz a segunda gravação da mesma assinatura
// perder a corrida — contada como falha, e a assinatura segue vencida para a próxima passagem.
//
// O horário é configurável e o padrão é de madrugada, no fuso do vencimento: a varredura compara
// datas, e rodar perto da virada do dia faria o resultado depender de minutos.
@Component
public class ChargeOverdueSubscriptionsJob {

    private static final Logger LOG = LoggerFactory.getLogger(ChargeOverdueSubscriptionsJob.class);

    private final ChargeOverdueSubscriptionsUseCase useCase;

    public ChargeOverdueSubscriptionsJob(final ChargeOverdueSubscriptionsUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "'useCase' should not be null");
    }

    @Scheduled(cron = "${subscription.charge-job.cron}", zone = "UTC")
    public void chargeOverdueSubscriptions() {
        LOG.info("varredura de assinaturas vencidas: começando");
        final var result = this.useCase.execute();
        LOG.info(
                "varredura de assinaturas vencidas: {} lidas, {} cobradas, {} recusadas, {} com falha",
                result.scanned(),
                result.charged(),
                result.refused(),
                result.failed());
        // Em WARN e uma por linha: é o que alguém lê quando o contador de falhas sobe.
        result.failures().forEach(failure -> LOG.warn("cobrança não concluída [{}]", failure));
    }
}
