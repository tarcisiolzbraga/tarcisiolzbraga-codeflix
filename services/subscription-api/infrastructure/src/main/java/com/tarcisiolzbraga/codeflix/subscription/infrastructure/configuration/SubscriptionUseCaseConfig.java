package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionGateways;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.cancel.CancelSubscriptionUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.cancel.DefaultCancelSubscriptionUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge.ChargeSubscriptionUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge.DefaultChargeSubscriptionUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.get.DefaultGetMySubscriptionUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.get.GetMySubscriptionUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.overdue.ChargeOverdueSubscriptionsUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.overdue.DefaultChargeOverdueSubscriptionsUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.subscribe.DefaultSubscribeUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.subscribe.SubscribeUseCase;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.payment.PaymentGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionGateway;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SubscriptionProperties.class)
public class SubscriptionUseCaseConfig {

    @Bean
    SubscriptionGateways subscriptionGateways(
            final AccountGateway accounts, final PlanGateway plans, final SubscriptionGateway subscriptions) {
        return new SubscriptionGateways(accounts, plans, subscriptions);
    }

    @Bean
    SubscribeUseCase subscribeUseCase(final SubscriptionGateways gateways) {
        return new DefaultSubscribeUseCase(gateways);
    }

    @Bean
    GetMySubscriptionUseCase getMySubscriptionUseCase(final SubscriptionGateways gateways) {
        return new DefaultGetMySubscriptionUseCase(gateways);
    }

    @Bean
    ChargeSubscriptionUseCase chargeSubscriptionUseCase(
            final SubscriptionGateways gateways,
            final PaymentGateway payments,
            final Clock clock,
            final SubscriptionProperties properties) {
        return new DefaultChargeSubscriptionUseCase(gateways, payments, clock, properties.toleranceDays());
    }

    @Bean
    CancelSubscriptionUseCase cancelSubscriptionUseCase(final SubscriptionGateways gateways) {
        return new DefaultCancelSubscriptionUseCase(gateways);
    }

    // A varredura das vencidas. Recebe o caso de uso de cobrar, e não os gateways de pagamento: ela
    // não cobra por conta própria, ela decide quem cobrar.
    @Bean
    ChargeOverdueSubscriptionsUseCase chargeOverdueSubscriptionsUseCase(
            final SubscriptionGateways gateways,
            final ChargeSubscriptionUseCase chargeSubscriptionUseCase,
            final Clock clock,
            final SubscriptionProperties properties) {
        return new DefaultChargeOverdueSubscriptionsUseCase(
                gateways, chargeSubscriptionUseCase, clock, properties.chargeJob().batchSize());
    }
}
