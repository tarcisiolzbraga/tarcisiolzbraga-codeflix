package com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.CallerSubscription;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionGateways;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.payment.Payment;
import com.tarcisiolzbraga.codeflix.subscription.domain.payment.PaymentGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.payment.Transaction;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.Subscription;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionCommand;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

// A cobrança da assinatura, e é aqui que o ciclo acontece: passou vira ativa, falhou e ainda há
// prazo vira incompleta, falhou e o prazo acabou vira cancelada.
//
// O "que dia é hoje" vem do relógio injetado, não de LocalDate.now(): é o que permite testar os
// três caminhos sem esperar o mês virar. E o fuso é o mesmo que o agregado usa para o vencimento —
// a referência compara com ZoneId.systemDefault() num ponto e ZoneOffset.UTC noutro, dentro do
// mesmo método, e aí o dia da virada depende de onde o processo roda.
public class DefaultChargeSubscriptionUseCase extends ChargeSubscriptionUseCase {

    private static final ZoneOffset DUE_DATE_ZONE = ZoneOffset.UTC;

    private final SubscriptionGateways gateways;
    private final PaymentGateway payments;
    private final Clock clock;
    private final int toleranceDays;

    public DefaultChargeSubscriptionUseCase(
            final SubscriptionGateways gateways,
            final PaymentGateway payments,
            final Clock clock,
            final int toleranceDays) {
        this.gateways = Objects.requireNonNull(gateways, "'gateways' should not be null");
        this.payments = Objects.requireNonNull(payments, "'payments' should not be null");
        this.clock = Objects.requireNonNull(clock, "'clock' should not be null");
        this.toleranceDays = toleranceDays;
    }

    @Override
    public ChargeSubscriptionOutput execute(final ChargeSubscriptionCommand command) {
        Objects.requireNonNull(command, "'command' should not be null");
        final var account = CallerSubscription.account(this.gateways, command.userId());
        final var subscription = CallerSubscription.of(this.gateways, command.userId());
        final var today = LocalDate.ofInstant(this.clock.instant(), DUE_DATE_ZONE);

        // Não vencida: nada a cobrar, e nada muda. Cobrar antes do vencimento seria cobrar duas
        // vezes no mesmo mês.
        if (subscription.dueDate().isAfter(today)) {
            return ChargeSubscriptionOutput.nothingDue(subscription);
        }

        // Sem endereço de cobrança o provedor não analisa a transação. É recusa de entrada, com
        // mensagem dizendo o que falta, e não uma tentativa que falharia lá.
        if (account.billingAddress() == null) {
            throw DomainException.with("the account has no billing address; set one before charging");
        }

        final var planId = subscription.planId();
        final var plan = this.gateways
                .plans()
                .planOfId(planId)
                .orElseThrow(() -> NotFoundException.with(Plan.class, planId));

        final var payment = new Payment(
                UUID.randomUUID().toString(), plan.price(), account.billingAddress(), command.creditCardToken());

        return switch (this.payments.processPayment(payment)) {
            case Transaction.Succeeded success -> renew(subscription, success);
            case Transaction.Failed failure -> refuse(subscription, failure, today);
        };
    }

    private ChargeSubscriptionOutput renew(final Subscription subscription, final Transaction.Succeeded success) {
        subscription.execute(new SubscriptionCommand.Renew(success.transactionId()));
        this.gateways.subscriptions().save(subscription);
        return ChargeSubscriptionOutput.succeeded(subscription, success.transactionId());
    }

    // Dentro do prazo, fica incompleta e a próxima tentativa acontece. Passado o prazo, cancela: é
    // o que impede uma assinatura nunca paga de ficar viva para sempre.
    private ChargeSubscriptionOutput refuse(
            final Subscription subscription, final Transaction.Failed failure, final LocalDate today) {
        final var daysLate = ChronoUnit.DAYS.between(subscription.dueDate(), today);
        if (daysLate <= this.toleranceDays) {
            subscription.execute(
                    new SubscriptionCommand.MarkIncomplete(failure.reason(), failure.transactionId()));
        } else {
            subscription.execute(new SubscriptionCommand.Cancel());
        }
        this.gateways.subscriptions().save(subscription);
        return ChargeSubscriptionOutput.failed(subscription, failure.transactionId(), failure.reason());
    }
}
