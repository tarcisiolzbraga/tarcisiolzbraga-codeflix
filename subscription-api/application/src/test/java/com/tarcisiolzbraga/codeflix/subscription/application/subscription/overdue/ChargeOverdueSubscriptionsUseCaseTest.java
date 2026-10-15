package com.tarcisiolzbraga.codeflix.subscription.application.subscription.overdue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionGateways;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge.ChargeSubscriptionCommand;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge.ChargeSubscriptionOutput;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge.ChargeSubscriptionUseCase;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.Account;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.UserId;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.PageRequest;
import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.Subscription;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionId;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

// A varredura das vencidas, com foco na paginação — que é a parte que pode errar em silêncio.
//
// O gateway aqui é **falso, não mock**: ele filtra, ordena e pagina como a consulta de verdade, e
// uma assinatura cobrada sai do resultado, porque cobrar move o vencimento. Com mock de `thenReturn`
// a paginação não seria exercitada, e é justamente nela que a varredura pularia gente ou não
// terminaria.
class ChargeOverdueSubscriptionsUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    private final FakeSubscriptions subscriptions = new FakeSubscriptions();
    private final FakeAccounts accounts = new FakeAccounts();
    private final StubCharge charge = new StubCharge();

    @Test
    void givenNothingOverdue_whenSweeps_thenItReadsNobody() {
        final var actual = useCase(10).execute();

        assertEquals(new ChargeOverdueSubscriptionsOutput(0, 0, 0, 0, List.of()), actual);
    }

    @Test
    void givenOneOverdue_whenSweeps_thenItIsCharged() {
        final var subscription = overdue(1);

        final var actual = useCase(10).execute();

        assertEquals(1, actual.scanned());
        assertEquals(1, actual.charged());
        assertEquals(List.of(subscription.id().getValue()), this.charge.chargedIds());
    }

    // Não vencida não entra na varredura: a consulta a deixa de fora, e cobrar antes do vencimento
    // seria cobrar duas vezes no mês.
    @Test
    void givenASubscriptionDueTomorrow_whenSweeps_thenItIsLeftAlone() {
        this.subscriptions.add(subscriptionDue(TODAY.plusDays(1), SubscriptionStatus.ACTIVE));

        final var actual = useCase(10).execute();

        assertEquals(0, actual.scanned());
    }

    @Test
    void givenACanceledSubscriptionLongOverdue_whenSweeps_thenItIsLeftAlone() {
        this.subscriptions.add(subscriptionDue(TODAY.minusDays(90), SubscriptionStatus.CANCELED));

        final var actual = useCase(10).execute();

        assertEquals(0, actual.scanned());
    }

    // O caso que a paginação pode errar: mais vencidas que o lote, todas cobradas.
    //
    // Cada cobrança move o vencimento, então a cobrada sai do resultado da consulta. Se a varredura
    // avançasse o deslocamento por página, ela pularia tantas quantas tivesse cobrado — aqui,
    // metade das vinte nunca seria vista.
    @Test
    void givenMoreOverdueThanTheBatch_whenAllAreCharged_thenEveryOneIsVisitedExactlyOnce() {
        final var expected = new LinkedHashSet<String>();
        for (var i = 0; i < 20; i++) {
            expected.add(overdue(i + 1).id().getValue());
        }

        final var actual = useCase(5).execute();

        assertEquals(20, actual.scanned());
        assertEquals(20, actual.charged());
        assertEquals(expected, Set.copyOf(this.charge.chargedIds()));
        assertEquals(20, this.charge.chargedIds().size(), "ninguém deveria ter sido cobrado duas vezes");
    }

    // O espelho do anterior: recusada **não** sai do resultado, porque o vencimento não muda. Se o
    // deslocamento não andasse, a varredura leria a mesma página para sempre.
    @Test
    void givenMoreOverdueThanTheBatch_whenAllAreRefused_thenItTerminatesAndVisitsEachOnce() {
        for (var i = 0; i < 12; i++) {
            this.charge.refuse(overdue(i + 1).id().getValue());
        }

        final var actual = useCase(5).execute();

        assertEquals(12, actual.scanned());
        assertEquals(0, actual.charged());
        assertEquals(12, actual.refused());
        assertEquals(12, this.charge.chargedIds().size(), "cada uma deveria ter sido tentada uma vez");
    }

    @Test
    void givenAMixOfChargedAndRefused_whenSweeps_thenEachIsVisitedOnceAndTheCountsAddUp() {
        for (var i = 0; i < 9; i++) {
            final var id = overdue(i + 1).id().getValue();
            if (i % 3 == 0) {
                this.charge.refuse(id);
            }
        }

        final var actual = useCase(4).execute();

        assertEquals(9, actual.scanned());
        assertEquals(6, actual.charged());
        assertEquals(3, actual.refused());
        assertEquals(9, this.charge.chargedIds().size());
    }

    // Uma que falha não para a varredura. Sem isto, a primeira conta sem meio de pagamento guardado
    // abortaria o lote e ninguém depois dela seria cobrado — em silêncio, porque o job roda sozinho.
    @Test
    void givenOneThatFails_whenSweeps_thenTheOthersAreStillCharged() {
        final var broken = overdue(5);
        overdue(4);
        overdue(3);
        this.charge.failOn(broken.id().getValue(), "the account has no stored payment method");

        final var actual = useCase(10).execute();

        assertEquals(3, actual.scanned());
        assertEquals(2, actual.charged());
        assertEquals(1, actual.failed());
        assertTrue(
                actual.failures().getFirst().contains("no stored payment method"),
                "o motivo deveria estar no relatório: " + actual.failures());
    }

    // Assinatura apontando para conta que não existe é falha, não exceção que sobe: o job roda
    // sozinho, e uma linha quebrada na base não pode parar a cobrança de todo mundo.
    @Test
    void givenASubscriptionWhoseAccountVanished_whenSweeps_thenItIsCountedAsFailed() {
        final var orphan = subscriptionDue(TODAY.minusDays(1), SubscriptionStatus.ACTIVE);
        this.subscriptions.add(orphan);

        final var actual = useCase(10).execute();

        assertEquals(1, actual.failed());
        assertTrue(actual.failures().getFirst().contains(orphan.id().getValue()));
    }

    // O relatório de falhas é limitado; o contador não. Devolver uma lista do tamanho da base seria
    // o oposto do que a paginação resolve.
    @Test
    void givenMoreFailuresThanTheReportHolds_whenSweeps_thenTheListIsCappedAndTheCountIsNot() {
        for (var i = 0; i < ChargeOverdueSubscriptionsOutput.MAX_REPORTED + 5; i++) {
            this.charge.failOn(overdue(i + 1).id().getValue(), "sem meio de pagamento");
        }

        final var actual = useCase(50).execute();

        assertEquals(ChargeOverdueSubscriptionsOutput.MAX_REPORTED + 5, actual.failed());
        assertEquals(ChargeOverdueSubscriptionsOutput.MAX_REPORTED, actual.failures().size());
    }

    @Test
    void givenABatchSizeThatIsNotPositive_whenBuilt_thenRefuses() {
        final var gateways = gateways();

        final var actual = assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultChargeOverdueSubscriptionsUseCase(gateways, this.charge, CLOCK, 0));

        assertEquals("'batchSize' should be positive", actual.getMessage());
    }

    private DefaultChargeOverdueSubscriptionsUseCase useCase(final int batchSize) {
        return new DefaultChargeOverdueSubscriptionsUseCase(gateways(), this.charge, CLOCK, batchSize);
    }

    private SubscriptionGateways gateways() {
        return new SubscriptionGateways(this.accounts, new UnusedPlans(), this.subscriptions);
    }

    // Vencida há `daysLate` dias, com conta própria registrada no gateway falso de contas.
    private Subscription overdue(final int daysLate) {
        final var subscription = subscriptionDue(TODAY.minusDays(daysLate), SubscriptionStatus.ACTIVE);
        this.accounts.add(subscription.accountId());
        this.subscriptions.add(subscription);
        return subscription;
    }

    private static Subscription subscriptionDue(final LocalDate dueDate, final SubscriptionStatus status) {
        return Subscription.with(
                SubscriptionId.unique(),
                1,
                AccountId.unique(),
                PlanId.unique(),
                status,
                dueDate,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    // A varredura não lê plano: quem lê é a cobrança, que aqui está dublada. Estourar se alguém o
    // chamar é o que torna isso uma afirmação, e não um detalhe de montagem.
    private static final class UnusedPlans implements PlanGateway {

        @Override
        public Optional<Plan> planOfId(final PlanId anId) {
            throw new AssertionError("a varredura não deveria ler plano");
        }

        @Override
        public Plan save(final Plan aPlan) {
            throw new AssertionError("a varredura não deveria gravar plano");
        }

        @Override
        public Pagination<Plan> activePlansOfPage(final PageRequest aPage) {
            throw new AssertionError("a varredura não deveria listar plano");
        }

        @Override
        public Pagination<Plan> plansOfPage(final PageRequest aPage) {
            throw new AssertionError("a varredura não deveria listar plano");
        }
    }

    // Filtra, ordena e pagina como a consulta de verdade. Cobrar remove do conjunto, porque mover o
    // vencimento tira a assinatura do resultado.
    private static final class FakeSubscriptions implements SubscriptionGateway {

        private final List<Subscription> stored = new ArrayList<>();

        void add(final Subscription subscription) {
            this.stored.add(subscription);
        }

        void remove(final String id) {
            this.stored.removeIf(it -> it.id().getValue().equals(id));
        }

        @Override
        public List<Subscription> chargeableSubscriptionsDueBy(
                final LocalDate aDate, final int limit, final int offset) {
            return this.stored.stream()
                    .filter(it -> !it.dueDate().isAfter(aDate))
                    .filter(it -> it.status() != SubscriptionStatus.CANCELED)
                    .sorted(Comparator.comparing(Subscription::dueDate)
                            .thenComparing(it -> it.id().getValue()))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public Optional<Subscription> latestSubscriptionOfAccount(final AccountId anAccountId) {
            return this.stored.stream()
                    .filter(it -> it.accountId().equals(anAccountId))
                    .findFirst();
        }

        @Override
        public Subscription save(final Subscription aSubscription) {
            return aSubscription;
        }
    }

    private final class FakeAccounts implements AccountGateway {

        private final Map<String, Account> byId = new HashMap<>();

        void add(final AccountId accountId) {
            final var account = Account.with(
                    accountId,
                    1,
                    new UserId(UUID.randomUUID().toString()),
                    new Email("assinante-%s@codeflix.com.br".formatted(UUID.randomUUID())),
                    AccountFixture.name(),
                    AccountFixture.document(),
                    AccountFixture.billingAddress(),
                    AccountFixture.paymentMethod());
            this.byId.put(accountId.getValue(), account);
        }

        @Override
        public Optional<Account> accountOfId(final AccountId anId) {
            return Optional.ofNullable(this.byId.get(anId.getValue()));
        }

        @Override
        public Optional<Account> accountOfEmail(final Email anEmail) {
            return Optional.empty();
        }

        @Override
        public Optional<Account> accountOfUserId(final UserId anUserId) {
            return this.byId.values().stream()
                    .filter(it -> it.userId().equals(anUserId))
                    .findFirst();
        }

        @Override
        public Account save(final Account anAccount) {
            return anAccount;
        }
    }

    // Cobra pelo userId, como o caso de uso de verdade, e devolve o que o teste programou. Remove do
    // gateway falso quando cobra, que é o efeito de o vencimento andar.
    private final class StubCharge extends ChargeSubscriptionUseCase {

        private final List<String> charged = new ArrayList<>();
        private final Set<String> toRefuse = new LinkedHashSet<>();
        private final Map<String, String> toFail = new HashMap<>();

        void refuse(final String subscriptionId) {
            this.toRefuse.add(subscriptionId);
        }

        void failOn(final String subscriptionId, final String reason) {
            this.toFail.put(subscriptionId, reason);
        }

        List<String> chargedIds() {
            return List.copyOf(this.charged);
        }

        @Override
        public ChargeSubscriptionOutput execute(final ChargeSubscriptionCommand command) {
            final var account = accounts.accountOfUserId(new UserId(command.userId())).orElseThrow();
            final var subscription = subscriptions.latestSubscriptionOfAccount(account.id()).orElseThrow();
            final var id = subscription.id().getValue();
            this.charged.add(id);
            if (this.toFail.containsKey(id)) {
                throw DomainException.with(this.toFail.get(id));
            }
            if (this.toRefuse.contains(id)) {
                return ChargeSubscriptionOutput.failed(subscription, "tx-1", "the card was refused");
            }
            subscriptions.remove(id);
            return ChargeSubscriptionOutput.succeeded(subscription, "tx-ok");
        }
    }
}
