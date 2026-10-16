package com.tarcisiolzbraga.codeflix.subscription.domain.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import com.tarcisiolzbraga.codeflix.subscription.domain.utils.InstantUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

// A assinatura de uma conta num plano, e o que acontece com ela mês a mês.
//
// O ciclo é: nasce em trial, vencendo em um mês. Na cobrança, passando vira ativa e o vencimento
// anda; falhando fica incompleta **sem o vencimento andar** — é isso que a mantém vencida e faz a
// próxima tentativa acontecer. Quando o prazo de tolerância acaba, quem decide cancelar é o caso de
// uso da cobrança, porque é ele que sabe que dia é hoje.
//
// O agregado não consulta relógio para decidir nada: ele registra datas. Quem compara "já venceu?"
// é o caso de uso, com a hora que recebe de fora — senão não haveria como testar a cobrança sem
// esperar um mês.
public class Subscription extends AggregateRoot<SubscriptionId> {

    // O fuso que decide o dia do vencimento. Fixo e documentado de propósito: com o fuso do
    // servidor, a mesma assinatura venceria em dias diferentes conforme onde o processo roda.
    private static final ZoneOffset DUE_DATE_ZONE = ZoneOffset.UTC;
    private static final int BILLING_PERIOD_MONTHS = 1;

    private AccountId accountId;
    private PlanId planId;
    private SubscriptionStatus status;
    private LocalDate dueDate;
    private Instant lastRenewDate;
    private String lastTransactionId;
    private Instant createdAt;
    private Instant updatedAt;

    private Subscription(
            final SubscriptionId anId,
            final int version,
            final AccountId anAccountId,
            final PlanId aPlanId,
            final SubscriptionStatus aStatus,
            final LocalDate aDueDate,
            final Instant lastRenewDate,
            final String lastTransactionId,
            final Instant createdAt,
            final Instant updatedAt) {
        super(anId, version);
        this.setAccountId(anAccountId);
        this.setPlanId(aPlanId);
        this.setStatus(aStatus);
        this.setDueDate(aDueDate);
        this.lastRenewDate = lastRenewDate;
        this.lastTransactionId = lastTransactionId;
        this.setCreatedAt(createdAt);
        this.setUpdatedAt(updatedAt);
    }

    public static Subscription newSubscription(
            final SubscriptionId anId, final AccountId anAccountId, final PlanId aPlanId) {
        final var now = InstantUtils.now();
        final var dueDate = LocalDate.ofInstant(now, DUE_DATE_ZONE).plusMonths(BILLING_PERIOD_MONTHS);
        final var aSubscription = new Subscription(
                anId,
                0,
                anAccountId,
                aPlanId,
                SubscriptionStatus.TRAILING,
                dueDate,
                null,
                null,
                now,
                now);
        aSubscription.registerEvent(SubscriptionCreated.from(aSubscription));
        return aSubscription;
    }

    public static Subscription with(
            final SubscriptionId anId,
            final int version,
            final AccountId anAccountId,
            final PlanId aPlanId,
            final SubscriptionStatus aStatus,
            final LocalDate aDueDate,
            final Instant lastRenewDate,
            final String lastTransactionId,
            final Instant createdAt,
            final Instant updatedAt) {
        return new Subscription(
                anId,
                version,
                anAccountId,
                aPlanId,
                aStatus,
                aDueDate,
                lastRenewDate,
                lastTransactionId,
                createdAt,
                updatedAt);
    }

    // Não toca na versão: ela é do lock otimista, e quem a incrementa é o UPDATE do repositório.
    //
    // A transição é pedida ao estado antes de qualquer outra mudança: recusando, nada no agregado
    // muda — nem vencimento, nem transação, nem evento.
    public void execute(final SubscriptionCommand... commands) {
        if (commands == null || commands.length == 0) {
            return;
        }

        for (final var command : commands) {
            switch (command) {
                case SubscriptionCommand.Renew c -> apply(c);
                case SubscriptionCommand.MarkIncomplete c -> apply(c);
                case SubscriptionCommand.Cancel c -> apply(c);
            }
        }

        this.setUpdatedAt(InstantUtils.now());
    }

    public AccountId accountId() {
        return this.accountId;
    }

    public PlanId planId() {
        return this.planId;
    }

    public SubscriptionStatus status() {
        return this.status;
    }

    public LocalDate dueDate() {
        return this.dueDate;
    }

    public Instant lastRenewDate() {
        return this.lastRenewDate;
    }

    public String lastTransactionId() {
        return this.lastTransactionId;
    }

    public Instant createdAt() {
        return this.createdAt;
    }

    public Instant updatedAt() {
        return this.updatedAt;
    }

    // Cobrança paga: ativa, empurra o vencimento um mês e registra quando e com qual transação.
    private void apply(final SubscriptionCommand.Renew command) {
        this.status = this.status.activate();
        this.dueDate = this.dueDate.plusMonths(BILLING_PERIOD_MONTHS);
        this.lastRenewDate = InstantUtils.now();
        this.lastTransactionId = command.transactionId();
        this.registerEvent(SubscriptionRenewed.from(this));
    }

    // Cobrança falhou e ainda há prazo. O vencimento fica onde está.
    private void apply(final SubscriptionCommand.MarkIncomplete command) {
        this.status = this.status.markIncomplete();
        this.lastTransactionId = command.transactionId();
        this.registerEvent(SubscriptionIncomplete.from(this, command.reason()));
    }

    private void apply(final SubscriptionCommand.Cancel command) {
        this.status = this.status.cancel();
        this.registerEvent(SubscriptionCanceled.from(this));
    }

    private void setAccountId(final AccountId accountId) {
        this.accountId = this.assertArgumentNotNull(accountId, "'accountId' should not be null");
    }

    private void setPlanId(final PlanId planId) {
        this.planId = this.assertArgumentNotNull(planId, "'planId' should not be null");
    }

    private void setStatus(final SubscriptionStatus status) {
        this.status = this.assertArgumentNotNull(status, "'status' should not be null");
    }

    private void setDueDate(final LocalDate dueDate) {
        this.dueDate = this.assertArgumentNotNull(dueDate, "'dueDate' should not be null");
    }

    private void setCreatedAt(final Instant createdAt) {
        this.createdAt = this.assertArgumentNotNull(createdAt, "'createdAt' should not be null");
    }

    private void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = this.assertArgumentNotNull(updatedAt, "'updatedAt' should not be null");
    }
}
