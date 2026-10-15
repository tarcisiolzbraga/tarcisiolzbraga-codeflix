package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// `toleranceDays`: quantos dias a assinatura fica incompleta antes de ser cancelada. Dois é o valor
// do curso, e o que ele representa é quanto tempo se dá a quem teve o cartão recusado para resolver.
//
// `chargeJob`: quando a varredura das vencidas roda, e de quantas em quantas. O lote existe porque a
// varredura passa pela base inteira, e carregar tudo numa lista cresce com o número de assinantes.
@ConfigurationProperties("subscription")
public record SubscriptionProperties(Integer toleranceDays, ChargeJob chargeJob) {

    private static final int DEFAULT_TOLERANCE_DAYS = 2;

    public SubscriptionProperties {
        toleranceDays = toleranceDays == null ? DEFAULT_TOLERANCE_DAYS : toleranceDays;
        if (toleranceDays < 0) {
            throw new IllegalStateException("'subscription.tolerance-days' should not be negative");
        }
        chargeJob = chargeJob == null ? new ChargeJob(null, null) : chargeJob;
    }

    public record ChargeJob(String cron, Integer batchSize) {

        // Três da manhã em UTC: a varredura compara datas, e rodar perto da virada do dia faria o
        // resultado depender de minutos.
        private static final String DEFAULT_CRON = "0 0 3 * * *";
        private static final int DEFAULT_BATCH_SIZE = 100;

        public ChargeJob {
            cron = cron == null || cron.isBlank() ? DEFAULT_CRON : cron;
            batchSize = batchSize == null ? DEFAULT_BATCH_SIZE : batchSize;
            if (batchSize <= 0) {
                throw new IllegalStateException("'subscription.charge-job.batch-size' should be positive");
            }
        }
    }
}
