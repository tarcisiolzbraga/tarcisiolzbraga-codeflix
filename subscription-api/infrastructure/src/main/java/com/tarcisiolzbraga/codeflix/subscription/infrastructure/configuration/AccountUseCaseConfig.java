package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.ConfirmSignUpGateways;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.ConfirmSignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.DefaultConfirmSignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.DefaultSignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.SignUpGateways;
import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.SignUpUseCase;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
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
}
