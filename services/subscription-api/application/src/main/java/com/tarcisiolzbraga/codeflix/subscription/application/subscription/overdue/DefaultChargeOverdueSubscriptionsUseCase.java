package com.tarcisiolzbraga.codeflix.subscription.application.subscription.overdue;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionGateways;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge.ChargeSubscriptionCommand;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge.ChargeSubscriptionUseCase;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.Subscription;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Varre as assinaturas vencidas e cobra cada uma. É o que faz o ciclo rodar sem ninguém chamar
// rota: antes dela, uma assinatura vencia e nada acontecia.
//
// Reaproveita o caso de uso de cobrar, sem atalho: é ele que decide o estado pelo resultado, e um
// segundo caminho de cobrança acabaria divergindo do primeiro. O preço é uma leitura a mais por
// assinatura — a varredura lê a conta para descobrir quem é, e a cobrança a lê de novo —, e numa
// operação que acontece uma vez por dia por assinatura isso é barato o bastante para não valer um
// caminho próprio.
//
// Em lotes, com limite e deslocamento: a varredura passa pela base inteira, e carregar tudo numa
// lista cresce com o número de assinantes.
//
// Não loga nada, e não é esquecimento: o módulo `application` depende só do `domain`, sem framework
// de log. Os motivos das falhas voltam no output, limitados, e quem os registra é o job.
//
// **Uma assinatura que falha não para a varredura.** Sem isto, a primeira conta sem meio de
// pagamento guardado abortaria o lote e ninguém depois dela seria cobrado — e o problema seria
// invisível, porque o job roda sozinho.
public class DefaultChargeOverdueSubscriptionsUseCase extends ChargeOverdueSubscriptionsUseCase {

    // O mesmo fuso do vencimento no agregado e na cobrança: um fuso diferente aqui faria a
    // varredura discordar da cobrança sobre o que está vencido.
    private static final ZoneOffset DUE_DATE_ZONE = ZoneOffset.UTC;

    private final SubscriptionGateways gateways;
    private final ChargeSubscriptionUseCase chargeSubscriptionUseCase;
    private final Clock clock;
    private final int batchSize;

    public DefaultChargeOverdueSubscriptionsUseCase(
            final SubscriptionGateways gateways,
            final ChargeSubscriptionUseCase chargeSubscriptionUseCase,
            final Clock clock,
            final int batchSize) {
        this.gateways = Objects.requireNonNull(gateways, "'gateways' should not be null");
        this.chargeSubscriptionUseCase =
                Objects.requireNonNull(chargeSubscriptionUseCase, "'chargeSubscriptionUseCase' should not be null");
        this.clock = Objects.requireNonNull(clock, "'clock' should not be null");
        if (batchSize <= 0) {
            throw new IllegalArgumentException("'batchSize' should be positive");
        }
        this.batchSize = batchSize;
    }

    @Override
    public ChargeOverdueSubscriptionsOutput execute() {
        final var today = LocalDate.ofInstant(this.clock.instant(), DUE_DATE_ZONE);
        final var failures = new ArrayList<String>();
        var scanned = 0;
        var charged = 0;
        var refused = 0;
        var failed = 0;

        // O deslocamento não anda com as páginas, e isto é deliberado: uma cobrança que passa move o
        // vencimento para o mês seguinte, então a assinatura sai do resultado da consulta. Avançar o
        // deslocamento faria a varredura pular tantas assinaturas quantas tivesse cobrado.
        //
        // Quem anda é o deslocamento das que **não** saíram — as que falharam ou ficaram incompletas
        // continuam vencidas e voltariam no mesmo lugar, e sem avançar o laço não terminaria.
        var offset = 0;
        List<Subscription> page;
        do {
            page = this.gateways.subscriptions().chargeableSubscriptionsDueBy(today, this.batchSize, offset);
            var stillDue = 0;
            for (final var subscription : page) {
                scanned++;
                switch (chargeOne(subscription, failures)) {
                    case CHARGED -> charged++;
                    case REFUSED -> {
                        refused++;
                        stillDue++;
                    }
                    case FAILED -> {
                        failed++;
                        stillDue++;
                    }
                }
            }
            offset += stillDue;
        } while (page.size() == this.batchSize);

        return new ChargeOverdueSubscriptionsOutput(scanned, charged, refused, failed, failures);
    }

    private Result chargeOne(final Subscription subscription, final List<String> failures) {
        try {
            final var account = this.gateways
                    .accounts()
                    .accountOfId(subscription.accountId())
                    .orElseThrow(() -> new IllegalStateException(
                            "a assinatura %s aponta para conta que não existe".formatted(subscription.id())));
            final var command = ChargeSubscriptionCommand.withStoredPaymentMethod(account.userId().value());
            return this.chargeSubscriptionUseCase.execute(command).charged() ? Result.CHARGED : Result.REFUSED;
        } catch (final RuntimeException exception) {
            // Anota e segue: o motivo mais comum é conta sem meio de pagamento guardado, que não é
            // defeito do sistema e não deve parar a varredura das outras.
            if (failures.size() < ChargeOverdueSubscriptionsOutput.MAX_REPORTED) {
                failures.add("%s: %s".formatted(subscription.id().getValue(), exception.getMessage()));
            }
            return Result.FAILED;
        }
    }

    private enum Result {
        CHARGED,
        REFUSED,
        FAILED
    }
}
