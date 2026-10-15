package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.subscription.application.account.access.DefaultGrantCatalogAccessUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.access.DefaultRevokeCatalogAccessUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.access.GrantCatalogAccessUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.access.RevokeCatalogAccessUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.billing.ChangeBillingAddressUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.billing.DefaultChangeBillingAddressUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.get.DefaultGetMyAccountUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.get.GetMyAccountUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.ConfirmSignUpGateways;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.ConfirmSignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.DefaultConfirmSignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.DefaultSignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.SignUpGateways;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.SignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.paymentmethod.DefaultStorePaymentMethodUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.paymentmethod.StorePaymentMethodUseCase;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.payment.PaymentGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.IdentityProviderGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUpGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpNotifier;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SignUpProperties.class)
public class AccountUseCaseConfig {

    @Bean
    SignUpGateways signUpGateways(
            final AccountGateway accounts,
            final PendingSignUpGateway pendingSignUps,
            final IdentityProviderGateway identityProvider,
            final SignUpNotifier notifier) {
        return new SignUpGateways(accounts, pendingSignUps, identityProvider, notifier);
    }

    @Bean
    ConfirmSignUpGateways confirmSignUpGateways(
            final AccountGateway accounts,
            final PendingSignUpGateway pendingSignUps,
            final IdentityProviderGateway identityProvider) {
        return new ConfirmSignUpGateways(accounts, pendingSignUps, identityProvider);
    }

    @Bean
    SignUpUseCase signUpUseCase(
            final SignUpGateways gateways, final Clock clock, final SignUpProperties properties) {
        return new DefaultSignUpUseCase(gateways, clock, properties.validationTtl());
    }

    @Bean
    ConfirmSignUpUseCase confirmSignUpUseCase(final ConfirmSignUpGateways gateways, final Clock clock) {
        return new DefaultConfirmSignUpUseCase(gateways, clock);
    }

    @Bean
    GetMyAccountUseCase getMyAccountUseCase(final AccountGateway accounts) {
        return new DefaultGetMyAccountUseCase(accounts);
    }

    @Bean
    ChangeBillingAddressUseCase changeBillingAddressUseCase(final AccountGateway accounts) {
        return new DefaultChangeBillingAddressUseCase(accounts);
    }

    @Bean
    StorePaymentMethodUseCase storePaymentMethodUseCase(
            final AccountGateway accounts, final PaymentGateway payments) {
        return new DefaultStorePaymentMethodUseCase(accounts, payments);
    }

    // Os dois acessos ao catálogo. Não têm rota: quem os chama é o consumidor dos eventos, porque
    // eles são consequência de a assinatura começar ou acabar, não coisa que alguém pede.
    @Bean
    GrantCatalogAccessUseCase grantCatalogAccessUseCase(
            final AccountGateway accounts, final IdentityProviderGateway identityProvider) {
        return new DefaultGrantCatalogAccessUseCase(accounts, identityProvider);
    }

    @Bean
    RevokeCatalogAccessUseCase revokeCatalogAccessUseCase(
            final AccountGateway accounts, final IdentityProviderGateway identityProvider) {
        return new DefaultRevokeCatalogAccessUseCase(accounts, identityProvider);
    }
}
